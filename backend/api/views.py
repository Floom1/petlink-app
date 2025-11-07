import os
from django.conf import settings
from rest_framework import viewsets, generics, permissions
from rest_framework.response import Response
from rest_framework.permissions import AllowAny, IsAuthenticated
from django.contrib.auth import get_user_model
from rest_framework.views import APIView
from .models import *
from .serializers import *
from django.core.files.storage import FileSystemStorage
from rest_framework.parsers import MultiPartParser, FormParser
from django.db.models import Q
from rest_framework.decorators import action


class AnimalViewSet(viewsets.ModelViewSet):
    queryset = Animal.objects.all()
    serializer_class = AnimalSerializer

    def list(self, request, *args, **kwargs):
        qs = Animal.objects.all()
        qs = qs.filter(status__is_available=True)

        species = request.query_params.get('species')
        breed = request.query_params.get('breed')
        gender = request.query_params.get('gender')
        age_min = request.query_params.get('age_min')
        age_max = request.query_params.get('age_max')
        price_min = request.query_params.get('price_min')
        price_max = request.query_params.get('price_max')
        is_hypo = request.query_params.get('is_hypoallergenic')
        child_friendly = request.query_params.get('child_friendly')
        space_req = request.query_params.get('space_requirements')
        is_ster = request.query_params.get('is_sterilized')
        has_vacc = request.query_params.get('has_vaccinations')

        if species:
            qs = qs.filter(breed__species_id=species)
        if breed:
            qs = qs.filter(breed_id=breed)
        if gender in ('M', 'F'):
            qs = qs.filter(gender=gender)
        if age_min is not None:
            try:
                qs = qs.filter(age__gte=float(age_min))
            except ValueError:
                pass
        if age_max is not None:
            try:
                qs = qs.filter(age__lte=float(age_max))
            except ValueError:
                pass
        min_val = None
        max_val = None
        try:
            if price_min is not None:
                min_val = float(price_min)
        except ValueError:
            min_val = None
        try:
            if price_max is not None:
                max_val = float(price_max)
        except ValueError:
            max_val = None

        if min_val is not None and min_val > 0:
            qs = qs.filter(price__gte=min_val)
            if max_val is not None:
                qs = qs.filter(price__lte=max_val)
        else:
            if max_val is not None:
                qs = qs.filter(Q(price__lte=max_val) | Q(price__isnull=True))
        if is_hypo in ('true', 'false'):
            qs = qs.filter(is_hypoallergenic=(is_hypo == 'true'))
        if child_friendly in ('true', 'false'):
            qs = qs.filter(child_friendly=(child_friendly == 'true'))
        if space_req in ('low', 'medium', 'high'):
            qs = qs.filter(space_requirements=space_req)
        if is_ster == 'true':
            qs = qs.filter(is_sterilized=True)
        if has_vacc == 'true':
            qs = qs.filter(has_vaccinations=True)

        serializer = self.get_serializer(qs, many=True)
        return Response(serializer.data)


class UserViewSet(viewsets.ModelViewSet):
    queryset = CustomUser.objects.all()
    serializer_class = UserSerializer


class SpeciesViewSet(viewsets.ModelViewSet):
    queryset = Species.objects.all()
    serializer_class = SpeciesSerializer


class BreedViewSet(viewsets.ModelViewSet):
    queryset = Breed.objects.all()
    serializer_class = BreedSerializer


class StatusViewSet(viewsets.ModelViewSet):
    queryset = Status.objects.all()
    serializer_class = StatusSerializer


class AnimalPhotoViewSet(viewsets.ModelViewSet):
    queryset = AnimalPhoto.objects.all()
    serializer_class = AnimalPhotoSerializer


class ServiceViewSet(viewsets.ModelViewSet):
    queryset = Service.objects.all()
    serializer_class = ServiceSerializer


class ServicePhotoViewSet(viewsets.ModelViewSet):
    queryset = ServicePhoto.objects.all()
    serializer_class = ServicePhotoSerializer


class TestResultViewSet(viewsets.ModelViewSet):
    queryset = TestResult.objects.all()
    serializer_class = TestResultSerializer

    @action(detail=True, methods=['get'], permission_classes=[IsAuthenticated])
    def recommendations(self, request, pk=None):
        test_result = self.get_object()
        recommendations = test_result.get_recommendations()
        return Response({
            'recommendations': recommendations
        })

    @action(detail=False, methods=['get'], permission_classes=[IsAuthenticated])
    def my_recommendations(self, request):
        try:
            test_result = TestResult.objects.get(user=request.user)
            recommendations = test_result.get_recommendations()
            return Response({
                'recommendations': recommendations
            })
        except TestResult.DoesNotExist:
            return Response({
                'error': 'Результаты теста не найдены'
            }, status=404)

    @action(detail=False, methods=['get'], permission_classes=[IsAuthenticated])
    def recommended_animals(self, request):
        try:
            test_result = TestResult.objects.get(user=request.user)
            recommendations = test_result.get_recommendations()

            if recommendations:
                filters = recommendations[0].get('filters', {})
            else:
                filters = {}

            qs = Animal.objects.filter(status__is_available=True)

            if 'space_requirements' in filters and filters['space_requirements']:
                qs = qs.filter(space_requirements=filters['space_requirements'])

            if 'child_friendly' in filters and filters['child_friendly'] is not None:
                qs = qs.filter(child_friendly=filters['child_friendly'])

            if 'is_hypoallergenic' in filters and filters['is_hypoallergenic'] is not None:
                qs = qs.filter(is_hypoallergenic=filters['is_hypoallergenic'])

            serializer = AnimalSerializer(qs, many=True)
            return Response(serializer.data)
        except TestResult.DoesNotExist:
            return Response({
                'error': 'Результаты теста не найдены'
            }, status=404)


class AnimalApplicationViewSet(viewsets.ModelViewSet):
    queryset = AnimalApplication.objects.all()
    serializer_class = AnimalApplicationSerializer


class ServiceApplicationViewSet(viewsets.ModelViewSet):
    queryset = ServiceApplication.objects.all()
    serializer_class = ServiceApplicationSerializer


User = get_user_model()

class RegisterAPI(generics.CreateAPIView):
    serializer_class = RegisterSerializer
    permission_classes = [AllowAny]

    def post(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.save()
        return Response({
            "user": UserSerializer(user, context=self.get_serializer_context()).data,
            "message": "User created succesfully. Now perform login to get your token"
        })


class UserAPI(generics.RetrieveAPIView):
    permission_classes = [permissions.IsAuthenticated]
    serializer_class = UserSerializer

    def get_object(self):
        return self.request.user


class UploadView(APIView):
    permission_classes = [IsAuthenticated]
    parser_classes = [MultiPartParser, FormParser]

    def post(self, request, *args, **kwargs):
        image = request.FILES.get('image')
        if not image:
            return Response({"error": "No image provided"}, status=400)

        upload_dir = os.path.join(settings.MEDIA_ROOT, 'profile_photos')
        os.makedirs(upload_dir, exist_ok=True)
        storage = FileSystemStorage(location=upload_dir)
        filename = storage.save(image.name, image)
        relative_path = os.path.join('profile_photos', filename).replace('\\', '/')

        url = request.build_absolute_uri(settings.MEDIA_URL + relative_path)
        return Response({"url": url}, status=201)

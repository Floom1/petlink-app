from rest_framework import viewsets
from .models import *
from .serializers import *


class AnimalViewSet(viewsets.ModelViewSet):
    queryset = Animal.objects.all()
    serializer_class = AnimalSerializer


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


class AnimalApplicationViewSet(viewsets.ModelViewSet):
    queryset = AnimalApplication.objects.all()
    serializer_class = AnimalApplicationSerializer


class ServiceApplicationViewSet(viewsets.ModelViewSet):
    queryset = ServiceApplication.objects.all()
    serializer_class = ServiceApplicationSerializer

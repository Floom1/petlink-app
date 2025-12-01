from rest_framework import serializers
from .models import *
from django.contrib.auth import get_user_model, authenticate
from django.core.exceptions import ValidationError


class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = CustomUser
        fields = (
            'id',
            'email',
            'full_name',
            'is_shelter',
            'shelter_name',
            'phone',
            'address',
            'photo_url'
        )
        extra_kwargs = {'password': {'write_only': True}}


class RegisterSerializer(serializers.ModelSerializer):
    class Meta:
        model = CustomUser
        fields = (
            'id',
            'email',
            'full_name',
            'password',
            'is_shelter',
            'shelter_name',
            'phone',
            'address',
            'photo_url'
        )
        extra_kwargs = {'password': {'write_only': True}}

    def create(self, validated_data):
        user = CustomUser.objects.create_user(
            email=validated_data['email'],
            full_name=validated_data['full_name'],
            password=validated_data['password'],
            is_shelter=validated_data.get('is_shelter', False),
            shelter_name=validated_data.get('shelter_name', ''),
            phone=validated_data.get('phone', ''),
            address=validated_data.get('address', ''),
            photo_url=validated_data.get('photo_url', '')
        )
        return user


class AnimalSerializer(serializers.ModelSerializer):
    class Meta:
        model = Animal
        fields = '__all__'


class SpeciesSerializer(serializers.ModelSerializer):
    class Meta:
        model = Species
        fields = '__all__'


class BreedSerializer(serializers.ModelSerializer):
    class Meta:
        model = Breed
        fields = '__all__'


class StatusSerializer(serializers.ModelSerializer):
    class Meta:
        model = Status
        fields = '__all__'


class AnimalPhotoSerializer(serializers.ModelSerializer):
    animal_id = serializers.IntegerField(source='animal.id', read_only=True)

    class Meta:
        model = AnimalPhoto
        fields = ['id', 'animal_id', 'photo_url', 'is_main', 'order']


class ServiceSerializer(serializers.ModelSerializer):
    class Meta:
        model = Service
        fields = '__all__'


class ServicePhotoSerializer(serializers.ModelSerializer):
    class Meta:
        model = ServicePhoto
        fields = '__all__'


class TestResultSerializer(serializers.ModelSerializer):
    user_id = serializers.IntegerField(source='user.id', read_only=True)

    class Meta:
        model = TestResult
        fields = ['id', 'user_id', 'residence_type', 'weekday_time', 'has_children', 'planned_move', 'pet_experience', 'has_allergies', 'created_at']
        read_only_fields = ('id', 'user_id', 'created_at')

    # def create(self, validated_data):
    #     validated_data['user'] = self.context['request'].user
    #     return super().create(validated_data)

    def create(self, validated_data):
        user = self.context['request'].user
        validated_data['user'] = user

        try:
            existing_result = TestResult.objects.get(user=user)
            for attr, value in validated_data.items():
                setattr(existing_result, attr, value)
            existing_result.save()
            return existing_result
        except TestResult.DoesNotExist:
            return super().create(validated_data)


class AnimalApplicationSerializer(serializers.ModelSerializer):
    animal_name = serializers.SerializerMethodField(read_only=True)
    buyer_name = serializers.SerializerMethodField(read_only=True)
    class Meta:
        model = AnimalApplication
        fields = ['id', 'animal', 'user', 'message', 'status', 'risk_info', 'created_at', 'updated_at', 'animal_name', 'buyer_name']
        read_only_fields = ('id', 'user', 'risk_info', 'created_at', 'updated_at', 'animal_name', 'buyer_name')

    def get_risks(self, obj):
        return obj.generate_risks()

    def get_animal_name(self, obj):
        try:
            return obj.animal.name
        except Exception:
            return None

    def get_buyer_name(self, obj):
        try:
            u = obj.user
            # если приют — показываем название приюта, иначе ФИО
            return u.shelter_name or u.full_name
        except Exception:
            return None

    def create(self, validated_data):
        request = self.context.get('request')
        if not request or not request.user.is_authenticated:
            raise serializers.ValidationError({'detail': 'Требуется аутентификация'})

        user = request.user
        animal = validated_data.get('animal')

        # Блокируем вторую активную заявку на того же зверя от того же пользователя
        if AnimalApplication.objects.filter(user=user, animal=animal, status='submitted').exists():
            raise serializers.ValidationError({'non_field_errors': ['У вас уже есть активная заявка на это животное']})

        # Принудительно устанавливаем статус submitted
        validated_data['status'] = 'submitted'
        validated_data['user'] = user
        instance = super().create(validated_data)

        # Снимок рисков на момент создания
        risks = instance.generate_risks()
        instance.risk_info = "\n".join(risks) if risks else ''
        instance.save(update_fields=['risk_info'])
        return instance



class ServiceApplicationSerializer(serializers.ModelSerializer):
    class Meta:
        model = ServiceApplication
        fields = '__all__'

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
    class Meta:
        model = AnimalPhoto
        fields = '__all__'


class ServiceSerializer(serializers.ModelSerializer):
    class Meta:
        model = Service
        fields = '__all__'


class ServicePhotoSerializer(serializers.ModelSerializer):
    class Meta:
        model = ServicePhoto
        fields = '__all__'


class TestResultSerializer(serializers.ModelSerializer):
    class Meta:
        model = TestResult
        fields = '__all__'


class AnimalApplicationSerializer(serializers.ModelSerializer):
    class Meta:
        model = AnimalApplication
        fields = '__all__'


class ServiceApplicationSerializer(serializers.ModelSerializer):
    class Meta:
        model = ServiceApplication
        fields = '__all__'

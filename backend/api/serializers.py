from rest_framework import serializers
from .models import *

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

from django.contrib import admin
from .models import *

@admin.register(CustomUser)
class CustomUserAdmin(admin.ModelAdmin):
    list_display = ('email', 'full_name', 'is_shelter', 'shelter_name', 'phone')
    list_filter = ('is_shelter', 'is_staff')
    search_fields = ('email', 'full_name')

@admin.register(Species)
class SpeciesAdmin(admin.ModelAdmin):
    list_display = ('name',)


@admin.register(Breed)
class BreedAdmin(admin.ModelAdmin):
    list_display = ('name', 'species')
    list_filter = ('species',)


@admin.register(Status)
class StatusAdmin(admin.ModelAdmin):
    list_display = ('name', 'is_available')


@admin.register(Animal)
class AnimalAdmin(admin.ModelAdmin):
    list_display = ('name', 'user', 'breed', 'age', 'status', 'is_hypoallergenic', 'child_friendly', 'space_requirements')
    list_filter = ('status', 'is_sterilized', 'has_vaccinations', 'is_hypoallergenic', 'child_friendly', 'space_requirements')
    search_fields = ('name', 'description')


@admin.register(AnimalPhoto)
class AnimalPhotoAdmin(admin.ModelAdmin):
    list_display = ('animal', 'is_main', 'order')


@admin.register(Service)
class ServiceAdmin(admin.ModelAdmin):
    list_display = ('name', 'user', 'price')
    search_fields = ('name', 'description')


@admin.register(ServicePhoto)
class ServicePhotoAdmin(admin.ModelAdmin):
    list_display = ('service', 'is_main', 'order')


@admin.register(TestResult)
class TestResultAdmin(admin.ModelAdmin):
    list_display = ('user', 'residence_type', 'weekday_time', 'has_children', 'planned_move', 'pet_experience', 'has_allergies')
    list_filter = ('residence_type', 'weekday_time', 'has_children', 'planned_move', 'pet_experience', 'has_allergies')
    search_fields = ('user__email', 'user__full_name')


@admin.register(AnimalApplication)
class AnimalApplicationAdmin(admin.ModelAdmin):
    list_display = ('animal', 'user', 'status', 'created_at')
    list_filter = ('status',)
    search_fields = ('message',)


@admin.register(ServiceApplication)
class ServiceApplicationAdmin(admin.ModelAdmin):
    list_display = ('service', 'user', 'status', 'created_at')
    list_filter = ('status',)
    search_fields = ('message',)
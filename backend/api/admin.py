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
    list_display = ('name', 'user', 'breed', 'age', 'status')
    list_filter = ('status', 'is_sterilized', 'has_vaccinations')
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
    list_display = ('user', 'has_yard', 'home_time', 'has_children')
    list_filter = ('has_yard', 'has_children')


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
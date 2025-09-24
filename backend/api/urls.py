from django.urls import include, path
from rest_framework.routers import DefaultRouter
from . import views


router = DefaultRouter()
router.register(r'animals', views.AnimalViewSet)
router.register(r'users', views.UserViewSet)
router.register(r'species', views.SpeciesViewSet)
router.register(r'breeds', views.BreedViewSet)
router.register(r'statuses', views.StatusViewSet)
router.register(r'animal_photos', views.AnimalPhotoViewSet)
router.register(r'services', views.ServiceViewSet)
router.register(r'service_photos', views.ServicePhotoViewSet)
router.register(r'tests', views.TestResultViewSet)
router.register(r'animal_apps', views.AnimalApplicationViewSet)
router.register(r'service_apps', views.ServiceApplicationViewSet)



urlpatterns = [
    path('', include(router.urls)),
]
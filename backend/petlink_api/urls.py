from django.contrib import admin
from django.urls import path, include
from rest_framework.authtoken import views
from api import views as api_views


urlpatterns = [
    path('admin/', admin.site.urls),
    path('api/', include('api.urls')),
    path('api-auth/', include('rest_framework.urls', namespace='rest_framework')),
    path('api-token-auth/', views.obtain_auth_token, name='api-token-auth'),
    path('api/auth/register/', api_views.RegisterAPI.as_view(), name='register'),
    path('api/auth/user/', api_views.UserAPI.as_view(), name='user'),
]

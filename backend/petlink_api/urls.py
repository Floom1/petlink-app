from django.contrib import admin
from django.urls import path, include
from rest_framework.authtoken import views
from api import views as api_views
from django.conf import settings
from django.conf.urls.static import static
from django.contrib.auth import views as auth_views

urlpatterns = [
    path('admin/', admin.site.urls),
    path('api/', include('api.urls')),
    path('api-auth/', include('rest_framework.urls', namespace='rest_framework')),
    path('api-token-auth/', views.obtain_auth_token, name='api-token-auth'),
    path('api/auth/register/', api_views.RegisterAPI.as_view(), name='register'),
    path('api/auth/user/', api_views.UserAPI.as_view(), name='user'),
    path('api/auth/password-reset/', api_views.PasswordResetRequestAPI.as_view(), name='password_reset'),
    path(
        'api/auth/password-reset-confirm/<uidb64>/<token>/',
        auth_views.PasswordResetConfirmView.as_view(
            template_name='registration/password_reset_confirm.html'
        ),
        name='password_reset_confirm',
    ),
    path(
        'api/auth/password-reset-complete/',
        auth_views.PasswordResetCompleteView.as_view(
            template_name='registration/password_reset_complete.html'
        ),
        name='password_reset_complete',
    ),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
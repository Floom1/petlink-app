from django.db import models
from django.contrib.auth.models import AbstractBaseUser, BaseUserManager, PermissionsMixin
from django.utils import timezone


class CustomUserManager(BaseUserManager):
    def create_user(self, email, password=None, **extra_fields):
        if not email:
            raise ValueError("У пользователя должен быть email")
        email = self.normalize_email(email)
        user = self.model(email=email, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, email, password=None, **extra_fields):
        extra_fields.setdefault('is_staff', True)
        extra_fields.setdefault('is_superuser', True)
        return self.create_user(email, password, **extra_fields)


class CustomUser(AbstractBaseUser, PermissionsMixin):
    email = models.EmailField(unique=True)
    full_name = models.CharField(max_length=100)
    is_shelter = models.BooleanField(default=False)
    shelter_name = models.CharField(max_length=255, blank=True, null=True)
    phone = models.CharField(max_length=20, blank=True, null=True)
    address = models.TextField(blank=True, null=True)
    photo_url = models.CharField(max_length=255, blank=True, null=True)
    created_at = models.DateTimeField(default=timezone.now)

    is_active = models.BooleanField(default=True)
    is_staff = models.BooleanField(default=False)

    objects = CustomUserManager()

    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['full_name']

    def __str__(self):
        return self.email


class Species(models.Model):
    name = models.CharField(max_length=50, unique=True)

    def __str__(self):
        return self.name


class Breed(models.Model):
    species = models.ForeignKey(Species, on_delete=models.CASCADE, related_name='breeds')
    name = models.CharField(max_length=100)

    class Meta:
        unique_together = ('species', 'name')
        verbose_name_plural = 'Breeds'

    def __str__(self):
        return f"{self.species.name} - {self.name}"


class Status(models.Model):
    name = models.CharField(max_length=50, unique=True)
    is_available = models.BooleanField(default=True)

    def __str__(self):
        return self.name

class Animal(models.Model):
    GENDER_CHOICES = [
        ('M', 'Мужской'),
        ('F', 'Женский'),
    ]

    user = models.ForeignKey(CustomUser, on_delete=models.CASCADE, related_name='animals')
    name = models.CharField(max_length=100, blank=True, null=True)
    breed = models.ForeignKey(Breed, on_delete=models.SET_NULL, blank=True, null=True, related_name='animals')
    age = models.DecimalField(max_digits=4, decimal_places=1, blank=True, null=True)
    gender = models.CharField(max_length=1, choices=GENDER_CHOICES)
    color = models.CharField(max_length=100)
    description = models.TextField(blank=True, null=False)
    status = models.ForeignKey(Status, on_delete=models.CASCADE)
    price = models.DecimalField(max_digits=10, decimal_places=2, blank=True, null=True)
    is_sterilized = models.BooleanField(default=False)
    has_vaccinations = models.BooleanField(default=False)
    habits = models.TextField(blank=True, null=True)
    is_hypoallergenic = models.BooleanField(default=False)
    child_friendly = models.BooleanField(default=False)
    space_requirements = models.CharField(max_length=20, choices=[
        ('low', 'Низкие'),
        ('medium', 'Средние'),
        ('high', 'Высокие'),
    ], default='medium')
    created_at = models.DateTimeField(default=timezone.now)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return self.name

class AnimalPhoto(models.Model):
    animal = models.ForeignKey(Animal, on_delete=models.CASCADE, related_name='photos')
    photo_url = models.CharField(max_length=255)
    is_main = models.BooleanField(default=False)
    order = models.IntegerField(default=0)

    class Meta:
        ordering = ['order']

    def __str__(self):
        return f"Фото для {self.animal.name}"


class Service(models.Model):
    user = models.ForeignKey(CustomUser, on_delete=models.CASCADE, related_name='services')
    name = models.CharField(max_length=100)
    description = models.TextField()
    price = models.DecimalField(max_digits=10, decimal_places=2, blank=True, null=True)
    created_at = models.DateTimeField(default=timezone.now)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return self.name


class ServicePhoto(models.Model):
    service = models.ForeignKey(Service, on_delete=models.CASCADE, related_name='photos')
    photo_url = models.CharField(max_length=255)
    is_main = models.BooleanField(default=False)
    order = models.IntegerField(default=0)

    class Meta:
        ordering = ['order']

    def __str__(self):
        return f"Фото для услуги {self.service.name}"


class TestResult(models.Model):
    RESIDENCE_CHOICES = [
        ('apartment', 'Квартира'),
        ('private', 'Частный дом'),
    ]

    WEEKDAY_TIME_CHOICES = [
        ('lt4', 'Менее 4 часов'),
        ('4_8', '4-8 часов'),
        ('gt8', 'Более 8 часов'),
    ]

    EXPERIENCE_CHOICES = [
        ('none', 'Нет опыта'),
        ('had_before', 'Имел(а) животное ранее'),
        ('now', 'В данный момент владею животным'),
    ]

    user = models.OneToOneField(CustomUser, on_delete=models.CASCADE, related_name='test_result')
    residence_type = models.CharField(max_length=20, choices=RESIDENCE_CHOICES, blank=True, null=True)
    weekday_time = models.CharField(max_length=10, choices=WEEKDAY_TIME_CHOICES, blank=True, null=True)
    has_children = models.BooleanField(default=False)
    planned_move = models.BooleanField(default=False)
    pet_experience = models.CharField(max_length=20, choices=EXPERIENCE_CHOICES, blank=True, null=True)
    has_allergies = models.BooleanField(default=False)
    created_at = models.DateTimeField(default=timezone.now)

    def __str__(self):
        return f"Результаты теста для {self.user.email}"

# Модели заявок
class AnimalApplication(models.Model):
    STATUS_CHOICES = [
        ('submitted', 'Отправлена'),
        ('approved', 'Одобрена'),
        ('rejected', 'Отклонена'),
    ]

    animal = models.ForeignKey(Animal, on_delete=models.CASCADE, related_name='applications')
    user = models.ForeignKey(CustomUser, on_delete=models.CASCADE, related_name='animal_applications')
    message = models.TextField(blank=True, null=True)
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='submitted')
    risk_info = models.TextField(blank=True, null=True)
    created_at = models.DateTimeField(default=timezone.now)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"Заявка на {self.animal.name} от {self.user.email}"

class ServiceApplication(models.Model):
    STATUS_CHOICES = [
        ('submitted', 'Отправлена'),
        ('approved', 'Одобрена'),
        ('rejected', 'Отклонена'),
    ]

    service = models.ForeignKey(Service, on_delete=models.CASCADE, related_name='applications')
    user = models.ForeignKey(CustomUser, on_delete=models.CASCADE, related_name='service_applications')
    message = models.TextField(blank=True, null=True)
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='submitted')
    created_at = models.DateTimeField(default=timezone.now)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"Заявка на {self.service.name} от {self.user.email}"
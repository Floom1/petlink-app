from django.db import models
from django.contrib.auth.models import AbstractBaseUser, BaseUserManager, PermissionsMixin
from django.utils import timezone
from django.db.models.signals import post_save
from django.dispatch import receiver


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

class Favorite(models.Model):
    user = models.ForeignKey(CustomUser, on_delete=models.CASCADE, related_name='favorites')
    animal = models.ForeignKey(Animal, on_delete=models.CASCADE, related_name='favorited_by')
    created_at = models.DateTimeField(default=timezone.now)

    class Meta:
        unique_together = ('user', 'animal')

    def __str__(self):
        return f"{self.user.email} 	 {self.animal.name}"

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

    def get_recommendations(self):
        """
        Генерирует рекомендации на основе результатов теста
        """
        is_apartment = self.residence_type == 'apartment'
        is_private_house = self.residence_type == 'private'
        little_time = self.weekday_time == 'lt4'
        much_time = self.weekday_time == 'gt8'
        no_experience = self.pet_experience == 'none'
        has_experience = self.pet_experience in ['had_before', 'now']

        recommendations = []

        # Проверяем комбинации с высоким приоритетом первыми

        # Шаблон 14: Планирует переезд + Есть аллергия (высший приоритет)
        if self.planned_move and self.has_allergies:
            recommendations.append({
                'template': 14,
                'message': "Рекомендуем хорьков или кошек, которые не вызывают аллергию и легко адаптируются к новым условиям.",
                'animals': ['Сфинкс', 'Хорек', 'Корниш-рекс', 'Девон-рекс', 'Балийская кошка'],
                'filters': {
                    'is_hypoallergenic': True
                }
            })

        # Шаблон 3: Планирует переезд (высокий приоритет)
        elif self.planned_move:
            recommendations.append({
                'template': 3,
                'message': "Внимание: вы планируете переезд в ближайшие 6 месяцев. Рекомендуем подождать с усыновлением до завершения переезда. Если хотите усыновить сейчас, рассмотрите кошек, которые дружелюбны к детям и легко адаптируются к новым условиям.",
                'animals': ['Рэгдолл', 'Мейн-кун', 'Сибирская кошка', 'Британская короткошерстная кошка', 'Скоттиш-фолд'],
                'filters': {
                    'child_friendly': True if self.has_children else None
                }
            })

        # Шаблон 1: Квартира, мало времени, нет опыта, есть аллергия (комплексный случай)
        elif is_apartment and little_time and no_experience and self.has_allergies:
            recommendations.append({
                'template': 1,
                'message': "Рекомендуем хорьков или кошек, которые не вызывают аллергию и не требуют много времени и могут оставаться одни в течение дня.",
                'animals': ['Сфинкс', 'Корниш-рекс', 'Девон-рекс', 'Хорек (Стандартный)', 'Балийская кошка'],
                'filters': {
                    'is_hypoallergenic': True,
                    'space_requirements': 'low'
                }
            })

        # Шаблон 4: Есть аллергия на шерсть животных (высокий приоритет)
        elif self.has_allergies:
            recommendations.append({
                'template': 4,
                'message': "Рекомендуем хорьков или кошек, которые не вызывают аллергию.",
                'animals': ['Сфинкс', 'Корниш-рекс', 'Девон-рекс', 'Балийская кошка', 'Ориентальная кошка'],
                'filters': {
                    'is_hypoallergenic': True
                }
            })

        # Шаблон 2: Дом с двором, много времени, есть дети
        elif is_private_house and much_time and self.has_children:
            recommendations.append({
                'template': 2,
                'message': "Подойдут спокойные породы собак или кошки, которые дружелюбны к детям. Собаки должны быть приучены к лотку и иметь дружелюбный характер.",
                'animals': ['Лабрадор-ретривер', 'Золотистый ретривер', 'Мейн-кун', 'Рэгдолл', 'Бернский зенненхунд'],
                'filters': {
                    'child_friendly': True,
                    'space_requirements': 'high'
                }
            })

        # Шаблон 5: Есть дети, много времени, опыт владения
        elif self.has_children and much_time and has_experience:
            recommendations.append({
                'template': 5,
                'message': "Подойдут спокойные породы собак или кошки, которые дружелюбны к детям.",
                'animals': ['Лабрадор-ретривер', 'Золотистый ретривер', 'Мейн-кун', 'Рэгдолл', 'Бернский зенненхунд'],
                'filters': {
                    'child_friendly': True
                }
            })

        # Шаблон 6: Квартира, много времени, нет аллергии
        elif is_apartment and much_time and not self.has_allergies:
            recommendations.append({
                'template': 6,
                'message': "Рекомендуем кошек, которые не требуют много внимания и могут быть активными, но при этом не требуют слишком много времени на уход.",
                'animals': ['Сиамская кошка', 'Персидская кошка', 'Британская короткошерстная кошка', 'Рэгдолл', 'Мейн-кун'],
                'filters': {
                    'space_requirements': 'medium'
                }
            })

        # Шаблон 7: Квартира, мало времени, опыт владения (но нет аллергии)
        elif is_apartment and little_time and has_experience and not self.has_allergies:
            recommendations.append({
                'template': 7,
                'message': "Рекомендуем хорьков или кошек с дружелюбным характером, подходящие для маленьких помещений.",
                'animals': ['Хорек (Стандартный)', 'Сиамская кошка', 'Девон-рекс', 'Корниш-рекс', 'Сфинкс'],
                'filters': {
                    'space_requirements': 'low'
                }
            })

        # Шаблон 8: Дом с двором, мало времени, нет аллергии
        elif is_private_house and little_time and not self.has_allergies:
            recommendations.append({
                'template': 8,
                'message': "Собаки подходят, но с ограничением на время. Рекомендуем собак с низкими требованиями к прогулкам.",
                'animals': ['Лабрадор-ретривер', 'Немецкая овчарка', 'Бернский зенненхунд', 'Кокер-спаниель', 'Ши-тцу'],
                'filters': {
                    'space_requirements': 'high'
                }
            })

        # Шаблон 9: Квартира, много времени, есть аллергия
        elif is_apartment and much_time and self.has_allergies:
            recommendations.append({
                'template': 9,
                'message': "Рекомендуем хорьков или кошек, которые не вызывают аллергию.",
                'animals': ['Хорек (Стандартный)', 'Сфинкс', 'Корниш-рекс', 'Девон-рекс', 'Балийская кошка'],
                'filters': {
                    'is_hypoallergenic': True,
                    'space_requirements': 'low'
                }
            })

        # Шаблон 10: Квартира, мало времени, нет опыта, нет аллергии
        elif is_apartment and little_time and no_experience and not self.has_allergies:
            recommendations.append({
                'template': 10,
                'message': "Рекомендуем кошек, которые не требуют много времени и могут оставаться одни в течение дня. Избегайте собак, требующих частых прогулок.",
                'animals': ['Британская короткошерстная кошка', 'Мейн-кун', 'Рэгдолл', 'Сибирская кошка', 'Скоттиш-фолд'],
                'filters': {
                    'space_requirements': 'low',
                    'child_friendly': True if self.has_children else None
                }
            })

        # Шаблон 11: Нет опыта, дом с двором, много времени
        elif no_experience and is_private_house and much_time:
            recommendations.append({
                'template': 11,
                'message': "Рекомендуем собак, которые дружелюбны и легко приучаются к новым условиям.",
                'animals': ['Лабрадор-ретривер', 'Золотистый ретривер', 'Бернский зенненхунд', 'Колли', 'Пудель'],
                'filters': {
                    'space_requirements': 'high'
                }
            })

        # Шаблон 12: Нет аллергии, квартира, много времени
        elif not self.has_allergies and is_apartment and much_time:
            recommendations.append({
                'template': 12,
                'message': "Кошки идеальны для квартиры, они не требуют много внимания и легко адаптируются к новому окружению.",
                'animals': ['Сиамская кошка', 'Персидская кошка', 'Британская короткошерстная кошка', 'Рэгдолл', 'Мейн-кун'],
                'filters': {
                    'space_requirements': 'medium'
                }
            })

        # Шаблон 13: Квартира, много времени, опыт владения
        elif is_apartment and much_time and has_experience:
            recommendations.append({
                'template': 13,
                'message': "Собаки и кошки будут хорошо чувствовать себя в доме без двора, если проводить достаточное время с ними.",
                'animals': ['Лабрадор-ретривер', 'Сиамская кошка', 'Мейн-кун', 'Золотистый ретривер', 'Сибирская кошка'],
                'filters': {
                    'space_requirements': 'medium'
                }
            })

        # Общая рекомендация (резервный шаблон)
        if not recommendations:
            recommendations.append({
                'template': 15,
                'message': "Лабрадоры - подходят для большинства условий. Рекомендуем также кошек и хорьков.",
                'animals': ['Лабрадор-ретривер', 'Сиамская кошка', 'Хорек', 'Британская короткошерстная кошка', 'Золотистый ретривер'],
                'filters': {}
            })

        return recommendations

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
    approved_at = models.DateTimeField(blank=True, null=True)

    def __str__(self):
        return f"Заявка на {self.animal.name} от {self.user.email}"

    def generate_risks(self):
        # Для приютов не показываем риски
        if self.user.is_shelter:
            return []

        # Берем данные из TestResult (OneToOne с пользователем)
        try:
            tr = self.user.test_result
        except TestResult.DoesNotExist:
            # По логике приложения тест обязателен, но на всякий случай возвращаем пусто
            return []

        risks = []

        # Жилье
        if tr.residence_type == 'apartment':
            risks.append('Квартира (ограниченное пространство)')

        # Время дома по будням
        if tr.weekday_time == 'lt4':
            risks.append('Проводит дома менее 4 часов')
        elif tr.weekday_time == '4_8':
            risks.append('Проводит дома 4–8 часов')
        # gt8 — не добавляем риск

        # Дети
        if tr.has_children:
            risks.append('Есть дети')

        # Переезд
        if tr.planned_move:
            risks.append('Планируется переезд')

        # Опыт владения питомцами
        if tr.pet_experience == 'none':
            risks.append('Нет опыта владения домашним питомцем')
        elif tr.pet_experience == 'now':
            risks.append('Сейчас владеет домашним питомцем')
        elif tr.pet_experience == 'had_before':
            risks.append('Ранее был домашний питомец')

        # Аллергии
        if tr.has_allergies:
            risks.append('Есть аллергии')

        return risks

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


class Notification(models.Model):
    TYPE_NEW_APPLICATION = 'NEW_APPLICATION'

    NOTIFICATION_TYPE_CHOICES = [
        (TYPE_NEW_APPLICATION, 'Новая заявка'),
    ]

    user = models.ForeignKey(CustomUser, on_delete=models.CASCADE, related_name='notifications')
    notification_type = models.CharField(max_length=50, choices=NOTIFICATION_TYPE_CHOICES)
    content = models.CharField(max_length=255)
    is_read = models.BooleanField(default=False)
    created_at = models.DateTimeField(default=timezone.now)
    application = models.ForeignKey(
        'AnimalApplication',
        on_delete=models.CASCADE,
        related_name='notifications',
        null=True,
        blank=True,
    )

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"Уведомление для {self.user.email}: {self.content[:50]}"


@receiver(post_save, sender=AnimalApplication)
def create_notification_for_new_application(sender, instance, created, **kwargs):
    if not created:
        return

    try:
        owner = instance.animal.user
    except Exception:
        return

    owner_id = getattr(owner, 'id', None)
    if owner_id is None:
        return

    # Если заявитель и создатель один и тот же - скип
    if owner_id == instance.user_id:
        return

    try:
        Notification.objects.create(
            user=owner,
            notification_type=Notification.TYPE_NEW_APPLICATION,
            content='На ваше объявление пришёл отклик!',
            application=instance,
        )
    except Exception:
        pass
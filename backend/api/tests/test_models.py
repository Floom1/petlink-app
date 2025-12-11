from django.test import TestCase
from django.contrib.auth import get_user_model

from api.models import Species, Breed, Status, Animal
# Модульное тестирование

class CustomUserModelTests(TestCase):
    def setUp(self):
        self.User = get_user_model()

    def test_create_user_with_email_successful(self):
        email = "TestUser@example.com"
        user = self.User.objects.create_user(
            email=email,
            password="test-pass-123",
            full_name="Test User",
        )

        self.assertEqual(user.email, email)
        self.assertEqual(user.full_name, "Test User")
        self.assertTrue(user.is_active)
        self.assertFalse(user.is_staff)
        self.assertTrue(user.check_password("test-pass-123"))

    def test_user_str_returns_email(self):
        user = self.User.objects.create_user(
            email="user@example.com",
            password="test-pass-123",
            full_name="User Name",
        )

        self.assertEqual(str(user), "user@example.com")

    def test_password_is_hashed(self):
        password = "password"
        user = self.User.objects.create_user(
            email="hash@example.com",
            password=password,
            full_name="Hash User",
        )

        self.assertNotEqual(user.password, password)
        self.assertTrue(user.check_password(password))

    def test_create_user_without_email_raises_error(self):
        with self.assertRaises(ValueError):
            self.User.objects.create_user(
                email="",
                password="pass123",
                full_name="No Email",
            )

class SimpleModelTests(TestCase):
    def test_species(self):
        species = Species.objects.create(name="Кошка")

        self.assertEqual(str(species), "Кошка")

    def test_breed(self):
        species = Species.objects.create(name="Собака")
        breed = Breed.objects.create(species=species, name="Лабрадор")

        self.assertEqual(str(breed), "Собака - Лабрадор")

    def test_animal_returns_name(self):
        User = get_user_model()
        user = User.objects.create_user(
            email="owner@example.com",
            password="test-pass-123",
            full_name="Owner User",
        )
        species = Species.objects.create(name="Кошка")
        breed = Breed.objects.create(
            species=species,
            name="Британская короткошерстная",
        )
        status = Status.objects.create(name="Пристраивается")

        animal = Animal.objects.create(
            user=user,
            name="Мурка",
            breed=breed,
            age=2,
            gender="F",
            color="Серый",
            description="Дружелюбная кошка",
            status=status,
        )

        self.assertEqual(str(animal), "Мурка")

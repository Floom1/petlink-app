from django.contrib.auth import get_user_model
from django.urls import reverse

from rest_framework import status
from rest_framework.authtoken.models import Token
from rest_framework.test import APITestCase

from api.models import Species, Breed, Status, Animal

# Интеграционное тестирование
class AuthAPITests(APITestCase):
    def setUp(self):
        self.User = get_user_model()
        self.register_url = reverse("register")
        self.token_url = reverse("api-token-auth")

    def test_register(self):
        payload = {
            "email": "newuser@example.com",
            "full_name": "New User",
            "password": "strong-pass-123",
        }

        response = self.client.post(self.register_url, payload, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("user", response.data)
        user_data = response.data["user"]
        self.assertEqual(user_data["email"], payload["email"])
        self.assertEqual(user_data["full_name"], payload["full_name"])
        self.assertTrue(self.User.objects.filter(email=payload["email"]).exists())

    def test_register_missing(self):
        payload = {
            "email": "incomplete@example.com",
            "password": "pass-1234",
        }

        response = self.client.post(self.register_url, payload, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("full_name", response.data)

    def test_register_duplicate(self):
        self.User.objects.create_user(
            email="duplicate@example.com",
            password="pass-1234",
            full_name="First User",
        )
        payload = {
            "email": "duplicate@example.com",
            "full_name": "Second User",
            "password": "pass-5678",
        }

        response = self.client.post(self.register_url, payload, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("email", response.data)

    def test_login(self):
        user = self.User.objects.create_user(
            email="login@example.com",
            password="pass-1234",
            full_name="Login User",
        )

        payload = {
            "username": "login@example.com",
            "password": "pass-1234",
        }

        response = self.client.post(self.token_url, payload, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("token", response.data)
        token_key = response.data["token"]
        self.assertTrue(Token.objects.filter(key=token_key, user=user).exists())

    def test_login_invalid(self):
        self.User.objects.create_user(
            email="wrongpass@example.com",
            password="correct-pass",
            full_name="User",
        )

        payload = {
            "username": "wrongpass@example.com",
            "password": "invalid-pass",
        }

        response = self.client.post(self.token_url, payload, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertNotIn("token", response.data)


class AnimalAPITests(APITestCase):
    def setUp(self):
        self.User = get_user_model()
        self.user = self.User.objects.create_user(
            email="owner@example.com",
            password="test-pass-123",
            full_name="Owner User",
        )
        self.other_user = self.User.objects.create_user(
            email="other@example.com",
            password="test-pass-123",
            full_name="Other User",
        )

        self.status_available = Status.objects.create(
            name="Пристраивается",
            is_available=True,
        )

        self.species_cat = Species.objects.create(name="Кошка")
        self.species_dog = Species.objects.create(name="Собака")
        self.cat_breed = Breed.objects.create(
            species=self.species_cat,
            name="Британская",
        )
        self.dog_breed = Breed.objects.create(
            species=self.species_dog,
            name="Лабрадор",
        )

        self.cat1 = Animal.objects.create(
            user=self.user,
            name="Кошка 1",
            breed=self.cat_breed,
            age=2,
            gender="F",
            color="Серый",
            description="Кошка 1",
            status=self.status_available,
        )
        self.dog_other_user = Animal.objects.create(
            user=self.other_user,
            name="Собака 1",
            breed=self.dog_breed,
            age=4,
            gender="M",
            color="Чёрный",
            description="Собака 1",
            status=self.status_available,
        )

        self.list_url = reverse("animal-list")

    def test_filter_by_species_returns_only_matching_animals(self):
        response = self.client.get(self.list_url, {"species": self.species_cat.id})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        returned_ids = {item["id"] for item in response.data}
        self.assertIn(self.cat1.id, returned_ids)
        self.assertNotIn(self.dog_other_user.id, returned_ids)

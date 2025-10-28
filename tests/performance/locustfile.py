"""Locust performance scenarios for exercising key ecommerce APIs."""

from datetime import datetime
import random

from locust import HttpUser, between, task


class EcommerceUser(HttpUser):
    wait_time = between(1, 3)

    def on_start(self):
        """Provision a cart so that order workflows have valid context."""
        response = self.client.post(
            "/order-service/api/carts",
            json={"userId": random.randint(1, 1000)},
        )
        if response.status_code == 200:
            self.cart_id = response.json().get("cartId")
        else:
            self.cart_id = None

    @task(3)
    def list_products(self):
        self.client.get("/product-service/api/products")

    @task(2)
    def list_orders(self):
        self.client.get("/order-service/api/orders")

    @task
    def create_order_and_cleanup(self):
        if not self.cart_id:
            # Lack of a cart means we cannot create an order; try to reprovision.
            self.on_start()
            return

        order_payload = {
            "orderDate": datetime.utcnow().strftime("%d-%m-%Y__%H:%M:%S:%f"),
            "orderDesc": "locust-order",
            "orderFee": round(random.uniform(10, 200), 2),
            "cart": {"cartId": self.cart_id},
        }
        with self.client.post(
            "/order-service/api/orders",
            json=order_payload,
            catch_response=True,
        ) as create_resp:
            if create_resp.status_code != 200:
                create_resp.failure(f"Order creation failed: {create_resp.status_code}")
                return

            order_id = create_resp.json().get("orderId")
            if not order_id:
                create_resp.failure("Missing orderId in response")
                return

            delete_resp = self.client.delete(f"/order-service/api/orders/{order_id}")
            if delete_resp.status_code != 200:
                create_resp.failure(f"Order cleanup failed: {delete_resp.status_code}")
            else:
                create_resp.success()

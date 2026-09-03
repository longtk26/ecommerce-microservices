"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useCartStore } from "@/store/cart-store";
import { useAuthStore } from "@/store/auth-store";
import { useCreateOrder } from "./use-create-order";
import {
  checkoutSchema,
  type TCheckoutFormValues,
  generateUserId,
} from "../schemas/checkout-schema";

export function useCheckoutForm() {
  const router = useRouter();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const items = useCartStore((state) => state.items);
  const shopId = useCartStore((state) => state.shopId);
  const shopName = useCartStore((state) => state.shopName);
  const clearCart = useCartStore((state) => state.clearCart);

  const user = useAuthStore((state) => state.user);

  const { mutateAsync: submitOrder, isPending } = useCreateOrder();

  const form = useForm<TCheckoutFormValues>({
    resolver: zodResolver(checkoutSchema),
    defaultValues: {
      customerEmail: user?.email || "",
    },
  });

  // Sync logged-in user email when available
  useEffect(() => {
    if (user?.email && !form.getValues("customerEmail")) {
      form.setValue("customerEmail", user.email);
    }
  }, [user?.email, form]);

  const onSubmit = async (values: TCheckoutFormValues) => {
    setErrorMessage(null);

    if (!shopId || items.length === 0) {
      setErrorMessage("Your cart is empty. Please add products before checking out.");
      return;
    }

    try {
      const orderPayload = {
        userId: user?.id || generateUserId(values.customerEmail),
        shopId: shopId,
        items: items.map((item) => ({
          productId: item.productId,
          quantity: item.quantity,
        })),
      };

      const response = await submitOrder(orderPayload);
      clearCart();
      router.push(`/orders/${response.orderId}`);
    } catch (err: unknown) {
      if (err && typeof err === "object" && "message" in err) {
        setErrorMessage(String(err.message));
      } else {
        setErrorMessage("Failed to place order. Please try again.");
      }
    }
  };

  return {
    form,
    onSubmit: form.handleSubmit(onSubmit),
    isSubmitting: isPending,
    errorMessage,
    items,
    shopId,
    shopName,
    isEmpty: items.length === 0,
  };
}


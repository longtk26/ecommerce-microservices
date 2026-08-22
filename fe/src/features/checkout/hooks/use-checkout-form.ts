"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useCartStore } from "@/store/cart-store";
import { useCreateOrder } from "./use-create-order";
import { checkoutSchema, type TCheckoutFormValues, generateUserId } from "../schemas/checkout-schema";

export function useCheckoutForm() {
  const router = useRouter();
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);

  const items = useCartStore((state) => state.items);
  const shopId = useCartStore((state) => state.shopId);
  const shopName = useCartStore((state) => state.shopName);
  const clearCart = useCartStore((state) => state.clearCart);

  const { mutateAsync: submitOrder, isPending } = useCreateOrder();

  const form = useForm<TCheckoutFormValues>({
    resolver: zodResolver(checkoutSchema),
    defaultValues: {
      customerName: "Alex Mercer",
    },
  });

  const watchedName = form.watch("customerName");
  const computedUserId = generateUserId(watchedName || "");

  const onSubmit = async (values: TCheckoutFormValues) => {
    setErrorMessage(null);

    if (!shopId || items.length === 0) {
      setErrorMessage("Your cart is empty. Please add products before checking out.");
      return;
    }

    try {
      const orderPayload = {
        userId: generateUserId(values.customerName),
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
    computedUserId,
    items,
    shopId,
    shopName,
    isEmpty: items.length === 0,
  };
}

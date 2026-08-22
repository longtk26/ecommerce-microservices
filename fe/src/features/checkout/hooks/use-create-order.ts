"use client";

import { useMutation } from "@tanstack/react-query";
import { createOrder } from "../api/checkout-api";
import type { TCreateOrderPayload, TCreateOrderResponse } from "@/types/order";

export function useCreateOrder() {
  return useMutation<TCreateOrderResponse, Error, TCreateOrderPayload>({
    mutationFn: (payload: TCreateOrderPayload) => createOrder(payload),
  });
}

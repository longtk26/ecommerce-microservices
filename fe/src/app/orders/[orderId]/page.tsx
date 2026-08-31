"use client";

import { useParams } from "next/navigation";
import { OrderStatusTracker } from "@/features/orders/components/OrderStatusTracker";

export default function OrderStatusPage() {
  const params = useParams();
  const orderId = Array.isArray(params.orderId) ? params.orderId[0] : params.orderId || "";

  return <OrderStatusTracker orderId={orderId} />;
}

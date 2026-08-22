export type TOrderStatus = "PENDING" | "COMPLETED" | "CANCELLED";

export type TCreateOrderItemPayload = {
  productId: string;
  quantity: number;
};

export type TCreateOrderPayload = {
  userId: string;
  shopId: string;
  items: TCreateOrderItemPayload[];
};

export type TCreateOrderResponse = {
  orderId: string;
  status: string;
  message: string;
};

export type TGetOrderItem = {
  productId: string;
  productName: string;
  unitPrice: number;
  quantity: number;
};

export type TGetOrderResponse = {
  orderId: string;
  userId: string;
  shopId: string;
  status: TOrderStatus | string;
  totalAmount: number;
  items: TGetOrderItem[];
};

export type TSagaStep = {
  id: string;
  label: string;
  description: string;
  service: string;
  status: "pending" | "processing" | "completed" | "failed";
};

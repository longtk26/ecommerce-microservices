export const orderKeys = {
  all: ["orders"] as const,
  detail: (orderId: string) => [...orderKeys.all, "detail", orderId] as const,
};

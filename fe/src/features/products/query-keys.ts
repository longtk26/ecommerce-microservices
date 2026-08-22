export const productKeys = {
  all: ["products"] as const,
  byShop: (shopId: string) => [...productKeys.all, "shop", shopId] as const,
  detail: (id: string) => [...productKeys.all, "detail", id] as const,
};

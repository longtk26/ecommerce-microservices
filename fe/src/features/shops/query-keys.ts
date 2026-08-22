export const shopKeys = {
  all: ["shops"] as const,
  lists: () => [...shopKeys.all, "list"] as const,
  detail: (id: string) => [...shopKeys.all, "detail", id] as const,
};

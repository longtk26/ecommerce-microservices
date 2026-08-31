"use client";

import { useParams } from "next/navigation";
import { Container } from "@/components/layout/Container";
import { ProductGrid } from "@/features/products/components/ProductGrid";
import { useShops } from "@/features/shops/hooks/use-shops";

export default function ShopDetailPage() {
  const params = useParams();
  const shopId = Array.isArray(params.shopId) ? params.shopId[0] : params.shopId || "";

  const { data: shops } = useShops();
  const shop = shops?.find((s) => s.id === shopId);

  return (
    <Container size="xl" className="py-8 sm:py-10">
      <ProductGrid shopId={shopId} shop={shop} />
    </Container>
  );
}

import { Container } from "@/components/layout/Container";
import { Stack } from "@/components/layout/Stack";
import { ShopHero } from "@/features/shops/components/ShopHero";
import { ShopList } from "@/features/shops/components/ShopList";

export default function HomePage() {
  return (
    <Container size="xl" className="py-8 sm:py-12">
      <Stack gap="2xl">
        <ShopList />
      </Stack>
    </Container>
  );
}

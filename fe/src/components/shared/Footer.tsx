import { Container } from "@/components/layout/Container";

export function Footer() {
  return (
    <footer className="mt-auto border-t border-border/60 bg-card/30 backdrop-blur-md py-12 text-sm text-muted-foreground">
      <Container size="xl">
        <div className="border-t border-border/40 pt-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs">
          <p>© {new Date().getFullYear()} ShopSaga. All rights reserved.</p>
          <p className="text-muted-foreground">Secure, seamless multi-vendor shopping experience.</p>
        </div>
      </Container>
    </footer>
  );
}


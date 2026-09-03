import Link from "next/link";
import { Card, CardHeader, CardTitle, CardDescription, CardFooter } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Store, ArrowRight, Sparkles } from "lucide-react";
import type { TShop } from "@/types/shop";

type ShopCardProps = {
  shop: TShop;
};

export function ShopCard({ shop }: ShopCardProps) {
  return (
    <Card className="group relative flex flex-col justify-between overflow-hidden border-border/80 bg-card/60 hover:bg-card/90 transition-all duration-300 hover:border-indigo-500/40 hover:shadow-xl hover:shadow-indigo-500/10 hover:-translate-y-1">
      {/* Decorative gradient blur backdrop */}
      <div className="absolute -top-12 -right-12 h-32 w-32 rounded-full bg-indigo-500/10 blur-2xl group-hover:bg-indigo-500/20 transition-all" />

      <CardHeader className="relative z-10 pb-4">
        <div className="flex items-start justify-between gap-4 mb-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500/20 to-purple-500/20 border border-indigo-500/30 text-indigo-400 group-hover:scale-105 transition-transform">
            {shop.logoUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={shop.logoUrl}
                alt={shop.name}
                className="h-8 w-8 object-contain rounded-xl"
              />
            ) : (
              <Store className="h-6 w-6" />
            )}
          </div>
          <Badge variant="glow" className="text-[11px]">
            <Sparkles className="h-3 w-3" />
            Verified Merchant
          </Badge>
        </div>

        <CardTitle className="text-xl font-bold text-foreground group-hover:text-primary transition-colors">
          {shop.name}
        </CardTitle>
        <CardDescription className="line-clamp-2 mt-2 text-xs leading-relaxed text-muted-foreground">
          {shop.description || "Discover verified products and exclusive offers from this shop."}
        </CardDescription>
      </CardHeader>

      <CardFooter className="relative z-10 pt-2 border-t border-border/40 mt-auto">
        <Link href={`/shops/${shop.id}`} className="w-full">
          <Button
            variant="outline"
            className="w-full justify-between group-hover:bg-primary group-hover:text-primary-foreground group-hover:border-transparent transition-all"
          >
            <span>Browse Products</span>
            <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
          </Button>
        </Link>
      </CardFooter>
    </Card>
  );
}


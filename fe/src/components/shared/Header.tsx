"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useCartStore } from "@/store/cart-store";
import { useAuthStore } from "@/store/auth-store";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { LoginDialog } from "@/features/auth/components/LoginDialog";
import {
  ShoppingBag,
  Store,
  Sparkles,
  LogOut,
  Mail,
  ShieldCheck,
} from "lucide-react";
import { Container } from "@/components/layout/Container";

export function Header() {
  const pathname = usePathname();

  // Zustand atomic selectors
  const items = useCartStore((state) => state.items);
  const openCart = useCartStore((state) => state.openCart);
  const shopName = useCartStore((state) => state.shopName);

  const user = useAuthStore((state) => state.user);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const logout = useAuthStore((state) => state.logout);

  const totalItemsCount = items.reduce((acc, item) => acc + item.quantity, 0);

  const userInitial = (user?.name || user?.email || "U").charAt(0).toUpperCase();

  return (
    <header className="sticky top-0 z-40 w-full border-b border-border/70 bg-background/80 backdrop-blur-xl transition-all">
      <Container size="xl">
        <div className="flex h-16 items-center justify-between gap-4">
          {/* Logo & Brand */}
          <div className="flex items-center gap-6">
            <Link
              href="/"
              className="flex items-center gap-2.5 group transition-transform active:scale-95"
            >
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-purple-500 shadow-md shadow-indigo-500/25 group-hover:shadow-indigo-500/50 transition-all">
                <Sparkles className="h-5 w-5 text-white" />
              </div>
              <div className="flex flex-col">
                <span className="text-base font-bold tracking-tight bg-gradient-to-r from-white via-slate-200 to-indigo-200 bg-clip-text text-transparent">
                  ShopSaga
                </span>
                <span className="text-[10px] uppercase font-semibold tracking-wider text-muted-foreground">
                  Multi-Vendor Marketplace
                </span>
              </div>
            </Link>

            {/* Navigation links */}
            <nav className="hidden md:flex items-center gap-1">
              <Link
                href="/"
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  pathname === "/"
                    ? "bg-secondary text-foreground"
                    : "text-muted-foreground hover:text-foreground hover:bg-secondary/50"
                }`}
              >
                <Store className="h-3.5 w-3.5" />
                All Shops
              </Link>
            </nav>
          </div>

          {/* Right Action Bar */}
          <div className="flex items-center gap-3">
            {shopName && (
              <Badge variant="glow" className="hidden sm:inline-flex gap-1.5 py-1">
                <Store className="h-3.5 w-3.5 text-indigo-400" />
                <span>
                  Active: <strong>{shopName}</strong>
                </span>
              </Badge>
            )}

            {/* Cart Button */}
            <Button
              onClick={openCart}
              variant="outline"
              size="default"
              className="relative gap-2 border-border/80 bg-card/80 hover:border-primary/50 shadow-sm"
              aria-label="Open Cart"
            >
              <ShoppingBag className="h-4 w-4 text-primary" />
              <span className="text-sm font-medium">Cart</span>
              {totalItemsCount > 0 && (
                <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-primary px-1 text-[11px] font-bold text-primary-foreground animate-in zoom-in-50 duration-200">
                  {totalItemsCount}
                </span>
              )}
            </Button>

            {/* Authentication Controls: User Popover or Login Dialog */}
            {isAuthenticated && user ? (
              <Popover>
                <PopoverTrigger asChild>
                  <button
                    type="button"
                    className="relative flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-tr from-indigo-600/20 via-indigo-500/20 to-purple-500/20 border border-primary/30 hover:border-primary/60 text-primary font-bold shadow-sm transition-all hover:scale-105 active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary cursor-pointer"
                    aria-label="User Profile"
                  >
                    <span className="text-xs font-bold text-foreground">
                      {userInitial}
                    </span>
                    <span className="absolute bottom-0 right-0 h-2.5 w-2.5 rounded-full bg-emerald-500 ring-2 ring-background" />
                  </button>
                </PopoverTrigger>
                <PopoverContent
                  align="end"
                  className="w-72 p-0 overflow-hidden shadow-2xl border-border/80 bg-card/95 backdrop-blur-2xl"
                >
                  <div className="flex flex-col p-4 bg-gradient-to-b from-secondary/50 to-transparent gap-3">
                    <div className="flex items-center gap-3">
                      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-gradient-to-tr from-indigo-600 to-purple-600 text-white font-bold text-sm shadow-md shadow-indigo-500/25">
                        {userInitial}
                      </div>
                      <div className="flex flex-col min-w-0 flex-1">
                        <span className="font-semibold text-foreground truncate text-sm">
                          {user.name || "User"}
                        </span>
                        <div className="flex items-center gap-1.5 text-xs text-muted-foreground truncate">
                          <Mail className="h-3 w-3 shrink-0" />
                          <span className="truncate">{user.email}</span>
                        </div>
                      </div>
                    </div>

                    {/* Role Badges */}
                    <div className="flex flex-wrap items-center gap-1.5 pt-1">
                      <span className="text-[11px] font-medium text-muted-foreground">
                        Role:
                      </span>
                      {user.roles && user.roles.length > 0 ? (
                        user.roles.map((role) => (
                          <Badge
                            key={role}
                            variant="glow"
                            className="text-[10px] px-2 py-0 uppercase font-semibold"
                          >
                            <ShieldCheck className="h-3 w-3 text-indigo-400" />
                            {role}
                          </Badge>
                        ))
                      ) : (
                        <Badge
                          variant="secondary"
                          className="text-[10px] px-2 py-0 uppercase"
                        >
                          BUYER
                        </Badge>
                      )}
                    </div>
                  </div>

                  <Separator />

                  <div className="p-2">
                    <Button
                      variant="ghost"
                      onClick={logout}
                      className="w-full justify-start gap-2.5 text-xs text-muted-foreground hover:text-destructive hover:bg-destructive/10 h-9 px-3 rounded-lg font-medium transition-colors"
                    >
                      <LogOut className="h-4 w-4" />
                      Sign Out
                    </Button>
                  </div>
                </PopoverContent>
              </Popover>
            ) : (
              <LoginDialog />
            )}
          </div>
        </div>
      </Container>
    </header>
  );
}


"use client";

import * as React from "react";
import { useShops } from "../hooks/use-shops";
import { ShopCard } from "./ShopCard";
import { Grid } from "@/components/layout/Grid";
import { Stack } from "@/components/layout/Stack";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { AlertCircle, RefreshCw, Store } from "lucide-react";

export function ShopList() {
  const { data: shops, isLoading, isError, error, refetch } = useShops();

  if (isLoading) {
    return (
      <Stack gap="lg">
        <div className="flex items-center justify-between">
          <Skeleton className="h-8 w-48" />
          <Skeleton className="h-4 w-24" />
        </div>
        <Grid cols={3} gap="md">
          {Array.from({ length: 3 }).map((_, i) => (
            <div
              key={i}
              className="h-64 rounded-2xl border border-border/40 bg-card/40 p-6 space-y-4"
            >
              <div className="flex justify-between">
                <Skeleton className="h-12 w-12 rounded-2xl" />
                <Skeleton className="h-5 w-24 rounded-full" />
              </div>
              <Skeleton className="h-6 w-3/4" />
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-10 w-full mt-auto rounded-lg" />
            </div>
          ))}
        </Grid>
      </Stack>
    );
  }

  if (isError) {
    return (
      <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-8 text-center space-y-4">
        <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-destructive/20 text-destructive">
          <AlertCircle className="h-6 w-6" />
        </div>
        <div className="space-y-1">
          <h3 className="text-base font-semibold text-foreground">Failed to Load Shops</h3>
          <p className="text-xs text-muted-foreground">
            {error instanceof Error ? error.message : "Could not connect to Inventory Service."}
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => refetch()} className="gap-2">
          <RefreshCw className="h-3.5 w-3.5" />
          Retry
        </Button>
      </div>
    );
  }

  if (!shops || shops.length === 0) {
    return (
      <div className="rounded-2xl border border-border bg-card/40 p-12 text-center space-y-3">
        <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-secondary text-muted-foreground">
          <Store className="h-6 w-6" />
        </div>
        <h3 className="text-base font-semibold text-foreground">No Shops Available</h3>
        <p className="text-xs text-muted-foreground max-w-sm mx-auto">
          No merchant shops were found in the database. Ensure the inventory database has been migrated and seeded.
        </p>
      </div>
    );
  }

  return (
    <Stack gap="lg">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-foreground">
            Available Shops
          </h2>
          <p className="text-xs text-muted-foreground">
            Select a store to explore its catalog and live inventory
          </p>
        </div>
        <span className="text-xs font-semibold text-muted-foreground bg-secondary px-3 py-1 rounded-full border border-border">
          {shops.length} {shops.length === 1 ? "Shop" : "Shops"}
        </span>
      </div>

      <Grid cols={3} gap="md">
        {shops.map((shop) => (
          <ShopCard key={shop.id} shop={shop} />
        ))}
      </Grid>
    </Stack>
  );
}

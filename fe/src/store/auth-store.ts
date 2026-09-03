import { create } from "zustand";
import { devtools, persist } from "zustand/middleware";
import { apiClient } from "@/lib/api-client";
import {
  setAuthCookies,
  deleteAuthCookies,
  getAuthCookie,
  getRefreshTokenCookie,
} from "@/lib/auth-actions";
import { loginApi } from "@/features/auth/api/auth-api";
import type { TUserRole, TAuthUser } from "@/features/auth/types/auth-types";

export type { TUserRole, TAuthUser };

export type TAuthState = {
  user: TAuthUser | null;
  token: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  setAuth: (user: TAuthUser, token: string, refreshToken?: string) => Promise<void>;
  updateTokens: (token: string, refreshToken?: string) => Promise<void>;
  loginWithCredentials: (email: string, pass: string) => Promise<void>;
  initAuth: () => Promise<void>;
  logout: () => Promise<void>;
};

export const useAuthStore = create<TAuthState>()(
  devtools(
    persist(
      (set, get) => ({
        user: null,
        token: null,
        refreshToken: null,
        isAuthenticated: false,

        setAuth: async (user, token, refreshToken) => {
          apiClient.setAuthToken(token);
          await setAuthCookies(token, refreshToken);
          set(
            { user, token, refreshToken: refreshToken ?? null, isAuthenticated: true },
            false,
            "auth/setAuth"
          );
        },

        updateTokens: async (token, refreshToken) => {
          const currentRefresh = refreshToken ?? get().refreshToken;
          apiClient.setAuthToken(token);
          await setAuthCookies(token, currentRefresh ?? undefined);
          set(
            { token, refreshToken: currentRefresh ?? null },
            false,
            "auth/updateTokens"
          );
        },

        initAuth: async () => {
          try {
            const [savedToken, savedRefreshToken] = await Promise.all([
              getAuthCookie(),
              getRefreshTokenCookie(),
            ]);

            if (savedToken) {
              apiClient.setAuthToken(savedToken);
              set(
                { token: savedToken, refreshToken: savedRefreshToken ?? null },
                false,
                "auth/initAuth"
              );
            } else if (savedRefreshToken) {
              set(
                { refreshToken: savedRefreshToken },
                false,
                "auth/initAuth"
              );
            }
          } catch (err) {
            console.warn("[AuthStore] Failed to initialize tokens from cookies:", err);
          }
        },

        loginWithCredentials: async (email, password) => {
          const response = await loginApi({ email, password });
          const mappedRoles = (response.user?.roles || ["BUYER"]).map(
            (r) => r.toUpperCase() as TUserRole
          );
          const user: TAuthUser = {
            id: response.user?.id || "cognito-user",
            email: response.user?.email || email,
            name: email.split("@")[0],
            roles: mappedRoles,
          };

          const tokenToUse = response.idToken || response.accessToken;
          apiClient.setAuthToken(tokenToUse);
          await setAuthCookies(tokenToUse, response.refreshToken);
          set(
            {
              user,
              token: tokenToUse,
              refreshToken: response.refreshToken ?? null,
              isAuthenticated: true,
            },
            false,
            "auth/loginWithCredentials"
          );
        },

        logout: async () => {
          apiClient.setAuthToken(null);
          await deleteAuthCookies();
          set(
            { user: null, token: null, refreshToken: null, isAuthenticated: false },
            false,
            "auth/logout"
          );
        },
      }),
      {
        name: "shopsaga-auth-v2",
        partialize: (state) => ({
          user: state.user,
          isAuthenticated: state.isAuthenticated,
        }),
      }
    ),
    { name: "auth-store" }
  )
);

export type TApiError = {
  status: number;
  message: string;
  data?: unknown;
};

type TRequestOptions = RequestInit & {
  _isRetry?: boolean;
};

class ApiClient {
  private authToken: string | null = null;
  private refreshPromise: Promise<string | null> | null = null;

  public setAuthToken(token: string | null): void {
    this.authToken = token;
  }

  public getAuthToken(): string | null {
    return this.authToken;
  }

  private async attemptTokenRefresh(): Promise<string | null> {
    try {
      const { useAuthStore } = await import("@/store/auth-store");
      const { getRefreshTokenCookie } = await import("@/lib/auth-actions");

      let refreshToken = useAuthStore.getState().refreshToken;
      if (!refreshToken) {
        refreshToken = (await getRefreshTokenCookie()) ?? null;
      }

      if (!refreshToken) {
        return null;
      }

      const response = await fetch("/api/auth/refresh", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify({ refreshToken }),
      });

      if (!response.ok) {
        // Refresh token is expired/invalid -> logout user cleanly
        await useAuthStore.getState().logout();
        return null;
      }

      const data = await response.json();
      const newAccessToken: string = data.accessToken;
      const newRefreshToken: string | undefined = data.refreshToken || refreshToken;

      if (newAccessToken) {
        this.setAuthToken(newAccessToken);
        await useAuthStore.getState().updateTokens(newAccessToken, newRefreshToken);
        return newAccessToken;
      }

      return null;
    } catch (err) {
      console.error("[ApiClient] Automatic token refresh error:", err);
      return null;
    } finally {
      this.refreshPromise = null;
    }
  }

  private async request<T>(url: string, options?: TRequestOptions): Promise<T> {
    const defaultHeaders: Record<string, string> = {
      "Content-Type": "application/json",
      Accept: "application/json",
    };

    if (this.authToken) {
      defaultHeaders["Authorization"] = `Bearer ${this.authToken}`;
    }

    const response = await fetch(url, {
      ...options,
      credentials: "same-origin", // Automatically sends HTTP cookies managed by Next.js Server Actions
      headers: {
        ...defaultHeaders,
        ...(options?.headers as Record<string, string> | undefined),
      },
    });

    // Automatically trigger token refresh on 401 Unauthorized for non-auth requests
    if (response.status === 401 && !options?._isRetry && !url.includes("/api/auth/")) {
      if (!this.refreshPromise) {
        this.refreshPromise = this.attemptTokenRefresh();
      }
      const newAccessToken = await this.refreshPromise;
      if (newAccessToken) {
        return this.request<T>(url, {
          ...options,
          _isRetry: true,
          headers: {
            ...(options?.headers as Record<string, string> | undefined),
            Authorization: `Bearer ${newAccessToken}`,
          },
        });
      }
    }

    if (!response.ok) {
      let errorMessage = `HTTP error ${response.status} (${response.statusText})`;
      let errorData: unknown = null;
      try {
        errorData = await response.json();
        if (errorData && typeof errorData === "object" && "message" in errorData) {
          errorMessage = String(errorData.message);
        }
      } catch {
        // Response is not JSON
      }

      const error: TApiError = {
        status: response.status,
        message: errorMessage,
        data: errorData,
      };
      throw error;
    }

    if (response.status === 204 || response.status === 202) {
      return null as T;
    }

    const text = await response.text();
    if (!text || !text.trim()) {
      return null as T;
    }

    try {
      return JSON.parse(text) as T;
    } catch {
      return text as unknown as T;
    }
  }

  public get<T>(url: string, params?: Record<string, string | number | boolean | undefined>): Promise<T> {
    let finalUrl = url;
    if (params) {
      const searchParams = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined) {
          searchParams.append(key, String(value));
        }
      });
      const query = searchParams.toString();
      if (query) {
        finalUrl += (url.includes("?") ? "&" : "?") + query;
      }
    }
    return this.request<T>(finalUrl, { method: "GET" });
  }

  public post<T>(url: string, body?: unknown): Promise<T> {
    return this.request<T>(url, {
      method: "POST",
      body: body ? JSON.stringify(body) : undefined,
    });
  }

  public put<T>(url: string, body?: unknown): Promise<T> {
    return this.request<T>(url, {
      method: "PUT",
      body: body ? JSON.stringify(body) : undefined,
    });
  }

  public delete<T>(url: string): Promise<T> {
    return this.request<T>(url, {
      method: "DELETE",
    });
  }
}

export const apiClient = new ApiClient();

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { authApi } from "../api/authApi";
import { AuthProvider } from "../features/auth/AuthProvider";
import type { User } from "../types/user";
import { LoginPage } from "./LoginPage";

vi.mock("../api/authApi", () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn(),
    me: vi.fn(),
  },
}));

const adminUser: User = {
  id: "admin-1",
  name: "Administrator",
  email: "admin@example.com",
  role: "ADMIN",
  active: true,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

function renderLoginPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/login"]}>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/admin/dashboard" element={<div>Dashboard page</div>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("LoginPage", () => {
  beforeEach(() => {
    vi.mocked(authApi.me).mockRejectedValue(new Error("no session"));
  });

  it("logs in and navigates to the role's home route on success", async () => {
    vi.mocked(authApi.login).mockResolvedValue({ token: "fake-token", user: adminUser });
    const user = userEvent.setup();
    renderLoginPage();

    await user.type(screen.getByLabelText("Email"), "admin@example.com");
    await user.type(screen.getByLabelText("Password"), "correct-password");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() => expect(screen.getByText("Dashboard page")).toBeInTheDocument());
    expect(authApi.login).toHaveBeenCalledWith({ email: "admin@example.com", password: "correct-password" });
  });

  it("shows an error message when login fails", async () => {
    vi.mocked(authApi.login).mockRejectedValue({
      isAxiosError: true,
      response: { data: { message: "Invalid email or password" } },
    });
    const user = userEvent.setup();
    renderLoginPage();

    await user.type(screen.getByLabelText("Email"), "admin@example.com");
    await user.type(screen.getByLabelText("Password"), "wrong-password");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid email or password");
  });

  it("shows validation errors instead of submitting when the form is empty", async () => {
    const user = userEvent.setup();
    renderLoginPage();

    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByText("Enter a valid email address")).toBeInTheDocument();
    expect(authApi.login).not.toHaveBeenCalled();
  });
});

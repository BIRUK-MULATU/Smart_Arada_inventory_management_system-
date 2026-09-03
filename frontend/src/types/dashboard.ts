import type { InventoryTransaction } from "./inventory";

export interface EmployeeSalesSummary {
  employeeId: string;
  employeeName: string;
  salesCount: number;
  revenue: number;
}

export interface RecentSaleSummary {
  id: string;
  employeeName: string;
  totalAmount: number;
  createdAt: string;
}

export interface DashboardSummary {
  totalProducts: number;
  totalStock: number;
  totalEmployees: number;
  lowStockCount: number;
  periodFrom: string;
  periodTo: string;
  periodSalesCount: number;
  periodRevenue: number;
  salesByEmployee: EmployeeSalesSummary[];
  recentSales: RecentSaleSummary[];
  recentInventoryMovements: InventoryTransaction[];
}

export interface DailySalesPoint {
  day: string;
  salesCount: number;
  revenue: number;
}

export interface TopProduct {
  productId: string;
  productName: string;
  unitsSold: number;
  revenue: number;
}

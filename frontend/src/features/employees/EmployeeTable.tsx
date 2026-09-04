import { Badge } from "../../components/Badge";
import { Button } from "../../components/Button";
import type { User } from "../../types/user";

interface EmployeeTableProps {
  employees: User[];
  onEdit: (employee: User) => void;
  onDeactivate: (employee: User) => void;
}

export function EmployeeTable({ employees, onEdit, onDeactivate }: EmployeeTableProps) {
  return (
    <div className="overflow-x-auto rounded-lg border border-ink-200 bg-white">
      <table className="min-w-full divide-y divide-ink-200 text-sm">
        <thead className="bg-ink-50">
          <tr>
            <th className="px-4 py-2 text-left font-medium text-ink-600">Name</th>
            <th className="px-4 py-2 text-left font-medium text-ink-600">Email</th>
            <th className="px-4 py-2 text-left font-medium text-ink-600">Role</th>
            <th className="px-4 py-2 text-left font-medium text-ink-600">Status</th>
            <th className="px-4 py-2" />
          </tr>
        </thead>
        <tbody className="divide-y divide-ink-100">
          {employees.map((employee) => (
            <tr key={employee.id}>
              <td className="px-4 py-2 font-medium text-ink-900">{employee.name}</td>
              <td className="px-4 py-2 text-ink-600">{employee.email}</td>
              <td className="px-4 py-2 text-ink-600">{employee.role}</td>
              <td className="px-4 py-2">
                <Badge tone={employee.active ? "success" : "neutral"}>{employee.active ? "Active" : "Inactive"}</Badge>
              </td>
              <td className="px-4 py-2 text-right">
                <div className="flex justify-end gap-2">
                  <Button variant="secondary" onClick={() => onEdit(employee)}>
                    Edit
                  </Button>
                  {employee.active && (
                    <Button variant="danger" onClick={() => onDeactivate(employee)}>
                      Deactivate
                    </Button>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

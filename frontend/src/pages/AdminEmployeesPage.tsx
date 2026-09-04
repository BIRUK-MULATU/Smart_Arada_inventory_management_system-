import { useState } from "react";
import { extractErrorMessage } from "../api/errors";
import { Button } from "../components/Button";
import { EmptyState } from "../components/EmptyState";
import { ErrorMessage } from "../components/ErrorMessage";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { Modal } from "../components/Modal";
import type { EmployeeFormValues } from "../features/employees/EmployeeForm";
import { EmployeeForm } from "../features/employees/EmployeeForm";
import { EmployeeTable } from "../features/employees/EmployeeTable";
import { useCreateEmployee, useDeactivateEmployee, useEmployees, useUpdateEmployee } from "../features/employees/useEmployees";
import type { User } from "../types/user";

type DialogState = { mode: "create" } | { mode: "edit"; employee: User } | null;

export function AdminEmployeesPage() {
  const { data: employees, isLoading, isError } = useEmployees();
  const createEmployee = useCreateEmployee();
  const updateEmployee = useUpdateEmployee();
  const deactivateEmployee = useDeactivateEmployee();
  const [dialog, setDialog] = useState<DialogState>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const handleSubmit = async (values: EmployeeFormValues) => {
    setFormError(null);
    try {
      if (dialog?.mode === "edit") {
        await updateEmployee.mutateAsync({
          id: dialog.employee.id,
          request: { name: values.name, email: values.email, role: values.role, active: values.active },
        });
      } else {
        await createEmployee.mutateAsync({
          name: values.name,
          email: values.email,
          password: values.password,
          role: values.role,
        });
      }
      setDialog(null);
    } catch (error) {
      setFormError(extractErrorMessage(error));
    }
  };

  const handleDeactivate = (employee: User) => {
    if (window.confirm(`Deactivate ${employee.name}? They will no longer be able to log in.`)) {
      deactivateEmployee.mutate(employee.id);
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-ink-900">Employees</h1>
        <Button onClick={() => setDialog({ mode: "create" })}>New employee</Button>
      </div>

      {isLoading && <LoadingSpinner />}
      {isError && <ErrorMessage message="Couldn't load employees." />}
      {employees && employees.length === 0 && <EmptyState message="No employees yet." />}
      {employees && employees.length > 0 && (
        <EmployeeTable employees={employees} onEdit={(employee) => setDialog({ mode: "edit", employee })} onDeactivate={handleDeactivate} />
      )}

      {dialog && (
        <Modal title={dialog.mode === "edit" ? "Edit employee" : "New employee"} onClose={() => setDialog(null)}>
          {formError && (
            <div className="mb-4">
              <ErrorMessage message={formError} />
            </div>
          )}
          <EmployeeForm
            employee={dialog.mode === "edit" ? dialog.employee : undefined}
            onSubmit={handleSubmit}
            onCancel={() => setDialog(null)}
          />
        </Modal>
      )}
    </div>
  );
}

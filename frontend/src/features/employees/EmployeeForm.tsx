import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";
import { Select } from "../../components/Select";
import type { User } from "../../types/user";

const baseSchema = {
  name: z.string().min(1, "Name is required"),
  email: z.email("Enter a valid email address"),
  role: z.enum(["ADMIN", "EMPLOYEE"]),
  active: z.boolean(),
};

// Both schemas produce the identical shape (password always a plain string) so the resolver's
// type is consistent whichever one is chosen conditionally at render time - editSchema just
// doesn't enforce the length constraint, since the field isn't rendered/submitted in edit mode.
const createSchema = z.object({ ...baseSchema, password: z.string().min(8, "Password must be at least 8 characters") });
const editSchema = z.object({ ...baseSchema, password: z.string() });

export type EmployeeFormValues = z.infer<typeof createSchema>;

interface EmployeeFormProps {
  employee?: User;
  onSubmit: (values: EmployeeFormValues) => Promise<void>;
  onCancel: () => void;
}

export function EmployeeForm({ employee, onSubmit, onCancel }: EmployeeFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<EmployeeFormValues>({
    resolver: zodResolver(employee ? editSchema : createSchema),
    defaultValues: {
      name: employee?.name ?? "",
      email: employee?.email ?? "",
      role: employee?.role ?? "EMPLOYEE",
      active: employee?.active ?? true,
      password: "",
    },
  });

  return (
    <form className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
      <Input label="Name" error={errors.name?.message} {...register("name")} />
      <Input label="Email" type="email" error={errors.email?.message} {...register("email")} />
      {!employee && (
        <Input label="Password" type="password" error={errors.password?.message} {...register("password")} />
      )}
      <Select label="Role" error={errors.role?.message} {...register("role")}>
        <option value="EMPLOYEE">Employee</option>
        <option value="ADMIN">Admin</option>
      </Select>
      {employee && (
        <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
          <input type="checkbox" className="h-4 w-4" {...register("active")} />
          Active
        </label>
      )}
      <div className="mt-2 flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Saving…" : "Save"}
        </Button>
      </div>
    </form>
  );
}

const variantStyles: Record<string, string> = {
  default: "bg-primary/10 text-primary",
  success: "bg-emerald-50 text-emerald-700 border border-emerald-200",
  warning: "bg-amber-50 text-amber-700 border border-amber-200",
  danger: "bg-red-50 text-red-700 border border-red-200",
  info: "bg-blue-50 text-blue-700 border border-blue-200",
  neutral: "bg-gray-100 text-gray-600 border border-gray-200",
  priority: "bg-purple-50 text-purple-700 border border-purple-200",
};

interface StatusBadgeProps {
  status: string;
  variant?: keyof typeof variantStyles;
}

export function StatusBadge({ status, variant = "default" }: StatusBadgeProps) {
  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium capitalize ${
        variantStyles[variant] ?? variantStyles.default
      }`}
    >
      {status}
    </span>
  );
}

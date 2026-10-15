import React from 'react';

interface FormFieldProps {
  label: string;
  hint?: string;
  children: React.ReactNode;
}

/*
 * A visible label above a form input. Placeholders disappear as soon
 * as a value is entered, so they cannot replace labels.
 */
export default function FormField({ label, hint, children }: FormFieldProps) {
  return (
    <label style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
      <span style={{ fontSize: 11, color: '#888' }}>{label}</span>
      {children}
      {hint && (
        <span style={{ fontSize: 10, color: '#666' }}>{hint}</span>
      )}
    </label>
  );
}
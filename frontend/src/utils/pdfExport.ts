import jsPDF from "jspdf";
import autoTable, { type RowInput } from "jspdf-autotable";

// jspdf-autotable patches the jsPDF instance with this at runtime; its own types don't expose it.
interface DocWithLastAutoTable {
  lastAutoTable?: { finalY: number };
}

const MARGIN = 40;
const INK_900: [number, number, number] = [23, 20, 15];
const INK_500: [number, number, number] = [107, 100, 89];
const INK_700: [number, number, number] = [53, 49, 43];
const INK_950: [number, number, number] = [11, 9, 6];
const GOLD_400: [number, number, number] = [212, 175, 106];
const INK_50: [number, number, number] = [247, 246, 244];

/** Starts a new PDF report with a consistent title/timestamp header. Returns the doc plus the Y
 * position where the first table should start. */
export function createReport(title: string, subtitle?: string): { doc: jsPDF; startY: number } {
  const doc = new jsPDF({ unit: "pt", format: "a4" });
  doc.setFontSize(16);
  doc.setTextColor(...INK_900);
  doc.text(title, MARGIN, 48);
  doc.setFontSize(10);
  doc.setTextColor(...INK_500);
  doc.text(`Generated ${new Date().toLocaleString()}`, MARGIN, 66);
  let startY = 88;
  if (subtitle) {
    doc.text(subtitle, MARGIN, 80);
    startY = 100;
  }
  return { doc, startY };
}

interface AddTableInput {
  title?: string;
  head: RowInput[];
  body: RowInput[];
  startY: number;
}

/** Draws a titled table starting at startY, styled to match the app's Onyx & Gold theme, and
 * returns the Y position the next element should start at. */
export function addTable(doc: jsPDF, { title, head, body, startY }: AddTableInput): number {
  let cursorY = startY;
  if (title) {
    doc.setFontSize(12);
    doc.setTextColor(...INK_900);
    doc.text(title, MARGIN, cursorY);
    cursorY += 12;
  }
  autoTable(doc, {
    head,
    body,
    startY: cursorY,
    margin: { left: MARGIN, right: MARGIN },
    styles: { fontSize: 9, cellPadding: 6, textColor: INK_700 },
    headStyles: { fillColor: INK_950, textColor: GOLD_400 },
    alternateRowStyles: { fillColor: INK_50 },
  });
  const finalY = (doc as unknown as DocWithLastAutoTable).lastAutoTable?.finalY ?? cursorY;
  return finalY + 24;
}

export function savePdf(doc: jsPDF, filename: string): void {
  doc.save(filename);
}

export function dateStamp(): string {
  return new Date().toISOString().slice(0, 10);
}

import { describe, expect, it } from "vitest";
import { addTable, createReport, dateStamp } from "./pdfExport";

describe("createReport", () => {
  it("starts a document with a title and a startY past the header", () => {
    const { doc, startY } = createReport("Test report");
    expect(doc.output("datauristring")).toContain("data:application/pdf");
    expect(startY).toBeGreaterThan(0);
  });

  it("reserves extra space when a subtitle is given", () => {
    const withoutSubtitle = createReport("Test report");
    const withSubtitle = createReport("Test report", "A subtitle");
    expect(withSubtitle.startY).toBeGreaterThan(withoutSubtitle.startY);
  });
});

describe("addTable", () => {
  it("returns a Y position further down the page than it started", () => {
    const { doc, startY } = createReport("Test report");
    const nextY = addTable(doc, {
      head: [["Name", "Value"]],
      body: [
        ["Widget", "9.99"],
        ["Gadget", "19.99"],
      ],
      startY,
    });
    expect(nextY).toBeGreaterThan(startY);
  });

  it("produces a non-empty, valid PDF byte stream", () => {
    const { doc, startY } = createReport("Test report");
    addTable(doc, { head: [["A"]], body: [["1"]], startY });
    const bytes = doc.output("arraybuffer");
    expect(bytes.byteLength).toBeGreaterThan(0);
    const header = new TextDecoder().decode(new Uint8Array(bytes, 0, 5));
    expect(header).toBe("%PDF-");
  });
});

describe("dateStamp", () => {
  it("returns today's date as YYYY-MM-DD", () => {
    expect(dateStamp()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });
});

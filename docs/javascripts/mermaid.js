mermaid.initialize({
  startOnLoad: false,
  securityLevel: "strict",
});

document$.subscribe(() => {
  mermaid.run({
    querySelector: ".mermaid",
  });
});

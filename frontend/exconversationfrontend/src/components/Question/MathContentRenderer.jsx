import { useMemo, useEffect, useRef } from 'react';

/**
 * Component để render nội dung có chứa công thức toán học (LaTeX)
 * Sử dụng MathJax để render LaTeX thành công thức toán học đẹp
 */
/**
 * Wrap LaTeX expressions với $ delimiters để MathJax nhận diện
 * Strategy: Tìm các math patterns phổ biến và wrap chúng
 */
function wrapLaTeXExpressions(text) {
  if (!text) return text;

  let result = text;
  
  // Strategy: Tìm các math patterns và wrap chúng
  // Patterns bao gồm: LaTeX commands, math expressions, functions, limits, etc.
  
  // Step 0: Wrap các LaTeX commands đã có sẵn nhưng chưa được wrap
  // Pattern: Tìm các LaTeX commands phức tạp và toàn bộ expressions chứa chúng
  // Strategy: Tìm các đoạn text chứa LaTeX commands và wrap toàn bộ đoạn đó
  
  // Pattern 1: Wrap các LaTeX expressions phức tạp (có thể chứa nhiều commands)
  // Ví dụ: \lim_{x \to -\infty}(x+3x-2)=+\infty, \frac{1km}{h}, \mathbb{R}
  result = result.replace(/([^$]|^)(\\[a-zA-Z]+(?:\{[^}]*\})*(?:\s*\\[a-zA-Z]+(?:\{[^}]*\})*)*(?:\([^)]*\))?(?:[=+\-*/().,;0-9a-zA-Z\s]*)?)([^$]|$)/g, (match, before, latexExpr, after) => {
    // Skip nếu đã được wrap hoặc không phải LaTeX
    if (before === '$' || after === '$' || !latexExpr.includes('\\')) {
      return match;
    }
    // Chỉ wrap nếu chứa LaTeX commands
    if (latexExpr.includes('\\')) {
      return `${before}$${latexExpr.trim()}$${after}`;
    }
    return match;
  });
  
  // Pattern 2: Wrap các LaTeX commands đơn giản hơn (fallback)
  const simpleLaTeXPatterns = [
    /\\frac\{[^}]+\}\{[^}]+\}/g,  // \frac{...}{...}
    /\\lim_\{[^}]+\}[^$]*/g,       // \lim_{...} và phần sau
    /\\mathbb\{[^}]+\}/g,         // \mathbb{...}
    /\\sqrt(?:\[[^\]]+\])?\{[^}]+\}/g,  // \sqrt[...]{...} or \sqrt{...}
    /\\to\s*[+-]?\\infty/g,       // \to \infty or \to -\infty
    /\\[a-zA-Z]+\{[^}]+\}/g,      // Any LaTeX command with braces
  ];
  
  simpleLaTeXPatterns.forEach(pattern => {
    result = result.replace(pattern, (match) => {
      // Nếu đã được wrap, giữ nguyên
      if (match.startsWith('$') && match.endsWith('$')) {
        return match;
      }
      // Wrap với $ delimiters
      return `$${match}$`;
    });
  });
  
  // Step 1: Wrap standalone LaTeX commands (phải có \)
  // Ví dụ: \infty, \mathbb{R}, \in, \forall
  result = result.replace(/([^$\\]|^)(\\[a-zA-Z]+(?:\{[^}]*\})*(?:\{[^}]*\})*)([^$]|$)/g, (match, before, latex, after) => {
    // Skip nếu đã được wrap
    if (before === '$' || after === '$') {
      return match;
    }
    // Wrap LaTeX command
    return `${before}$${latex}$${after}`;
  });
  
  // Step 2: Wrap superscripts/subscripts (^{...} hoặc _{...})
  result = result.replace(/([^$]|^)(\^{[^}]+}|\_{[^}]+})([^$]|$)/g, (match, before, supSub, after) => {
    if (before === '$' || after === '$') {
      return match;
    }
    return `${before}$${supSub}$${after}`;
  });
  
  // Step 3: Wrap fractions first (phải có dấu /)
  // Pattern: (ax-4)/(bx+c) hoặc ax-4/bx+c
  result = result.replace(/([^$]|^)(\(?[a-zA-Z0-9+\-*()]+\)?\s*\/\s*\(?[a-zA-Z0-9+\-*()]+\)?)([^$]|$)/g, (match, before, fraction, after) => {
    if (before === '$' || after === '$') {
      return match;
    }
    // Convert to LaTeX fraction format
    const parts = fraction.split(/\s*\/\s*/);
    if (parts.length === 2) {
      const num = parts[0].replace(/[()]/g, '');
      const den = parts[1].replace(/[()]/g, '');
      return `${before}$\\frac{${num}}{${den}}$${after}`;
    }
    return match;
  });
  
  // Step 4: Wrap math expressions phổ biến (không có \ nhưng là math)
  // Patterns: y=x+3x-9x+1, fx=xx-1x+2, etc.
  // Pattern: Tìm các đoạn có dạng: variable=expression
  result = result.replace(/([^$]|^)([a-zA-Z]+=[a-zA-Z0-9+\-*/().,; ]+)([^$]|$)/g, (match, before, mathExpr, after) => {
    // Skip nếu đã được wrap hoặc quá dài (có thể là câu văn)
    if (before === '$' || after === '$' || mathExpr.length > 100) {
      return match;
    }
    // Chỉ wrap nếu có dấu = và chứa các ký tự math, không phải là câu văn
    if (mathExpr.includes('=') && /[+\-*/().,;0-9]/.test(mathExpr) && !/[àáảãạăắằẳẵặâấầẩẫậèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộùúủũụưứừửữựỳýỷỹỵđ]/.test(mathExpr)) {
      return `${before}$${mathExpr.trim()}$${after}`;
    }
    return match;
  });
  
  // Step 5: Wrap limit expressions (limx→-00, limx→+00, limx→∞)
  result = result.replace(/([^$]|^)(lim\s*[a-zA-Z]+\s*→\s*([-+]?[0-9]+|∞|00|\\infty))([^$]|$)/g, (match, before, limitExpr, target, after) => {
    if (before === '$' || after === '$') {
      return match;
    }
    // Extract variable and target
    const varMatch = limitExpr.match(/lim\s*([a-zA-Z]+)/);
    if (varMatch) {
      const varName = varMatch[1];
      let targetValue = target;
      if (target === '00' || target === '+00') {
        targetValue = '\\infty';
      } else if (target === '-00') {
        targetValue = '-\\infty';
      }
      return `${before}$\\lim_{${varName} \\to ${targetValue}}$${after}`;
    }
    // Fallback: simple conversion
    const latexLimit = limitExpr.replace(/→/g, '\\to').replace(/00/g, '\\infty');
    return `${before}$${latexLimit}$${after}`;
  });
  
  // Step 6: Wrap intervals và sets ([-1;1], (-00;0), (2;+00), (-∞,1), (1,+∞))
  result = result.replace(/([^$]|^)([\[\(][-+]?(?:[0-9]+|∞|00|\\infty);[-+]?(?:[0-9]+|∞|00|\\infty)[\]\)])([^$]|$)/g, (match, before, interval, after) => {
    if (before === '$' || after === '$') {
      return match;
    }
    // Convert -00 to -\infty, +00 to +\infty, ∞ to \infty
    let latexInterval = interval
      .replace(/-00/g, '-\\infty')
      .replace(/\+00/g, '+\\infty')
      .replace(/([^\\])∞/g, '$1\\infty');
    return `${before}$${latexInterval}$${after}`;
  });
  
  // Step 7: Wrap function notation (f(x), g(x), etc.)
  result = result.replace(/([^$]|^)([a-zA-Z]+\s*\([a-zA-Z0-9+\-*/().,; ]+\))([^$]|$)/g, (match, before, funcExpr, after) => {
    if (before === '$' || after === '$' || funcExpr.length > 50) {
      return match;
    }
    // Only wrap if it looks like a mathematical function
    if (/^[a-zA-Z]+\s*\([^)]+\)$/.test(funcExpr) && /[+\-*/().,;0-9]/.test(funcExpr)) {
      return `${before}$${funcExpr.trim()}$${after}`;
    }
    return match;
  });
  
  // Step 8: Wrap combined math expressions (chứa cả text và LaTeX commands)
  // Ví dụ: "y=x^2+3x" hoặc "lim x → -∞"
  result = result.replace(/([^$]*[a-zA-Z0-9]+[^$]*\\[a-zA-Z]+[^$]*)/g, (match) => {
    // Nếu đã được wrap, giữ nguyên
    if (match.trim().startsWith('$') || match.includes('$$')) {
      return match;
    }
    // Wrap nếu chứa LaTeX
    if (match.includes('\\')) {
      return `$${match.trim()}$`;
    }
    return match;
  });
  
  // Clean up: Remove duplicate $ và fix spacing
  result = result.replace(/\$\$+/g, '$');
  result = result.replace(/\$\s+\$/g, ' ');
  result = result.replace(/\$\$([^$]+)\$\$/g, '$$$1$$'); // Fix double wrapping
  
  return result;
}

function MathContentRenderer({ content, className = '' }) {
  const containerRef = useRef(null);
  const mathJaxLoaded = useRef(false);

  // Process content: Backend đã convert OMML sang LaTeX, cần wrap với delimiters để MathJax render
  const processedContent = useMemo(() => {
    if (!content) {
      console.log('MathContentRenderer: No content provided');
      return 'No content';
    }

    console.log('MathContentRenderer: Original content length:', content.length);
    console.log('MathContentRenderer: Original content preview:', content.substring(0, 200));

    let processed = content;

    // 1. Remove OMML/XML tags nếu còn sót lại (fallback cho dữ liệu cũ)
    processed = processed.replace(/<omml[^>]*>[\s\S]*?<\/omml>/gi, '');
    processed = processed.replace(/<xml-fragment[^>]*>[\s\S]*?<\/xml-fragment>/gi, '');
    processed = processed.replace(/<[^>]+>/g, '');
    processed = processed.replace(/xmlns[^=]*="[^"]*"/gi, '');

    // 2. Decode XML entities
    const entityMap = {
      '&lt;': '<',
      '&gt;': '>',
      '&amp;': '&',
      '&quot;': '"',
      '&apos;': "'",
      '&nbsp;': ' '
    };
    Object.entries(entityMap).forEach(([entity, char]) => {
      processed = processed.replace(new RegExp(entity, 'g'), char);
    });

    // 3. Detect và wrap LaTeX expressions với $ delimiters để MathJax render
    const beforeWrap = processed;
    processed = wrapLaTeXExpressions(processed);
    
    if (beforeWrap !== processed) {
      console.log('MathContentRenderer: Content was wrapped with LaTeX delimiters');
      console.log('MathContentRenderer: Wrapped content preview:', processed.substring(0, 300));
    } else {
      console.log('MathContentRenderer: No LaTeX expressions detected, content unchanged');
    }

    // 4. Clean up whitespace nhưng giữ nguyên LaTeX delimiters
    processed = processed.replace(/\s+/g, ' ');
    processed = processed.trim();

    console.log('MathContentRenderer: Final processed content length:', processed.length);
    return processed;
  }, [content]);

  // Load và setup MathJax
  useEffect(() => {
    if (!containerRef.current || !processedContent || processedContent === 'No content') return;

    const loadAndRenderMathJax = () => {
      // Configure MathJax
      if (!window.MathJax) {
        window.MathJax = {
          tex: {
            inlineMath: [['$', '$'], ['\\(', '\\)']],
            displayMath: [['$$', '$$'], ['\\[', '\\]']],
            processEscapes: true,
            processEnvironments: true
          },
          options: {
            skipHtmlTags: ['script', 'noscript', 'style', 'textarea', 'pre']
          }
        };

        // Load MathJax from CDN
        if (!document.getElementById('mathjax-script')) {
          const script = document.createElement('script');
          script.id = 'mathjax-script';
          script.src = 'https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js';
          script.async = true;
          document.head.appendChild(script);
          
          script.onload = () => {
            mathJaxLoaded.current = true;
            renderMath();
          };
        }
      } else {
        mathJaxLoaded.current = true;
        renderMath();
      }
    };

    const renderMath = () => {
      if (window.MathJax && window.MathJax.typesetPromise && containerRef.current) {
        console.log('MathContentRenderer: Rendering MathJax for element:', containerRef.current);
        window.MathJax.typesetPromise([containerRef.current])
          .then(() => {
            console.log('MathContentRenderer: MathJax rendering completed');
          })
          .catch((err) => {
            console.error('MathContentRenderer: MathJax typeset error:', err);
          });
      } else {
        console.warn('MathContentRenderer: MathJax not available or container not ready');
        if (!window.MathJax) {
          console.warn('MathContentRenderer: window.MathJax is not defined');
        }
        if (!containerRef.current) {
          console.warn('MathContentRenderer: containerRef.current is null');
        }
      }
    };

    // Small delay to ensure DOM is ready
    const timer = setTimeout(loadAndRenderMathJax, 100);
    return () => clearTimeout(timer);
  }, [processedContent]);

  // Escape HTML để tránh XSS, nhưng giữ LaTeX delimiters và expressions
  const escapedContent = useMemo(() => {
    if (!processedContent) return '';
    
    // Strategy: Escape HTML nhưng giữ nguyên $ delimiters và LaTeX expressions
    // Split by $ delimiters, escape text parts, keep math parts unchanged
    const parts = processedContent.split(/(\$[^$]*\$)/g);
    
    return parts.map((part, index) => {
      // Nếu là LaTeX expression (có $ delimiters), giữ nguyên
      if (part.startsWith('$') && part.endsWith('$')) {
        return part;
      }
      // Nếu không, escape HTML
      return part
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
    }).join('');
  }, [processedContent]);

  return (
    <div 
      ref={containerRef}
      className={`math-content ${className}`}
      style={{ 
        whiteSpace: 'pre-wrap',
        wordWrap: 'break-word',
        lineHeight: '1.8',
        fontSize: '15px',
        fontFamily: 'inherit',
        color: 'inherit'
      }}
      dangerouslySetInnerHTML={{ __html: escapedContent }}
    />
  );
}

export default MathContentRenderer;

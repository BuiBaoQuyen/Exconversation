import { useMemo, useEffect, useRef } from 'react';

/**
 * Component để render nội dung có chứa công thức toán học (MathML)
 * Sử dụng MathJax để render MathML thành công thức toán học đẹp
 */

/**
 * Process MathML content: đảm bảo MathML tags có namespace đúng và sẵn sàng để render
 */
function processMathMLContent(text) {
  if (!text) return text;

  let result = text;
  
  // 1. Đảm bảo các <math> tags có namespace đúng
  // Pattern: <math> hoặc <math ...> nhưng chưa có xmlns
  result = result.replace(/<math(\s|>)/g, (match, after) => {
    if (match.includes('xmlns=')) {
      return match; // Đã có namespace
    }
    return `<math xmlns="http://www.w3.org/1998/Math/MathML"${after}`;
  });
  
  // 2. Remove OMML tags nếu còn sót lại (fallback cho dữ liệu cũ)
  result = result.replace(/<omml[^>]*>[\s\S]*?<\/omml>/gi, '');
  
  // 3. Decode XML entities nếu cần
  const entityMap = {
    '&lt;': '<',
    '&gt;': '>',
    '&amp;': '&',
    '&quot;': '"',
    '&apos;': "'",
    '&nbsp;': ' '
  };
  Object.entries(entityMap).forEach(([entity, char]) => {
    result = result.replace(new RegExp(entity, 'g'), char);
  });
  
  return result;
}

/**
 * Chuyển imagePath từ DB thành URL API để hiển thị ảnh
 */
function buildImageUrl(imagePath) {
  if (!imagePath) return '';
  let path = imagePath.replace(/^\.\//, '').replace(/^\.\\/, '');
  path = path.replace(/^uploads[\/\\]images[\/\\]/, '');
  path = path.replace(/^\.\/uploads[\/\\]images[\/\\]/, '');
  const imagesIndex = path.indexOf('images/');
  if (imagesIndex >= 0) {
    path = path.substring(imagesIndex + 'images/'.length);
  }
  path = path.replace(/\\/g, '/');
  return `http://localhost:8080/api/images/${path}`;
}

/**
 * Thay placeholder [IMAGE:{id}] bằng thẻ <img> tương ứng
 * images: array ImageDTO { id, imagePath, description }
 */
function replaceImagePlaceholders(content, images) {
  if (!content || !images || images.length === 0) return content;
  return content.replace(/\[IMAGE:(\d+)\]/g, (match, idStr) => {
    const id = parseInt(idStr, 10);
    const img = images.find(i => i.id === id);
    if (!img) return match; // placeholder nhưng không tìm thấy ảnh → giữ nguyên
    const src = buildImageUrl(img.imagePath);
    const alt = img.description || 'Question image';
    return `<img src="${src}" alt="${alt}" class="inline-question-image" style="max-width:100%;display:block;margin:8px auto;" onerror="this.style.display='none'" />`;
  });
}

function MathContentRenderer({ content, images = [], className = '' }) {
  const containerRef = useRef(null);
  const mathJaxLoaded = useRef(false);

  // Process content: thay placeholder ảnh + đảm bảo MathML namespace đúng
  const processedContent = useMemo(() => {
    if (!content) {
      console.log('MathContentRenderer: No content provided');
      return 'No content';
    }

    console.log('MathContentRenderer: Original content length:', content.length);
    console.log('MathContentRenderer: Original content preview:', content.substring(0, 200));

    // Bước 1: Thay [IMAGE:{id}] → <img> inline
    let processed = replaceImagePlaceholders(content, images);

    // Bước 2: Process MathML content: đảm bảo namespace và format đúng
    processed = processMathMLContent(processed);

    console.log('MathContentRenderer: Processed content length:', processed.length);
    console.log('MathContentRenderer: Processed content preview:', processed.substring(0, 300));
    
    return processed;
  }, [content, images]);

  // Load và setup MathJax
  useEffect(() => {
    if (!containerRef.current || !processedContent || processedContent === 'No content') return;

    const loadAndRenderMathJax = () => {
      // Configure MathJax để hỗ trợ cả LaTeX và MathML
      if (!window.MathJax) {
        window.MathJax = {
          loader: { load: ['input/mml', 'output/chtml'] },
          tex: {
            inlineMath: [['$', '$'], ['\\(', '\\)']],
            displayMath: [['$$', '$$'], ['\\[', '\\]']],
            processEscapes: true,
            processEnvironments: true
          },
          mml: {
            parseAs: 'html'  // Parse MathML từ HTML
          },
          options: {
            skipHtmlTags: ['script', 'noscript', 'style', 'textarea', 'pre'],
            // Enable MathML rendering
            renderActions: {
              addMenu: [],
              checkLoading: []
            }
          }
        };

        // Load MathJax from CDN (hỗ trợ cả LaTeX và MathML)
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

  // Escape HTML để tránh XSS, nhưng giữ nguyên MathML tags và <img> tags đã được nhúng
  const escapedContent = useMemo(() => {
    if (!processedContent) return '';
    
    // Split by MathML tags và <img> tags (đã được nhúng sẵn), escape phần còn lại
    const safeTagPattern = /(<math[^>]*>[\s\S]*?<\/math>|<img\s[^>]*?\/>|<img\s[^>]*?>)/gi;
    const parts = processedContent.split(safeTagPattern);
    
    return parts.map((part) => {
      // Nếu là MathML hoặc img tag, giữ nguyên
      if (part.trim().startsWith('<math') || part.trim().startsWith('<img')) {
        return part;
      }
      // Escape HTML cho phần text thuần
      return part
        .replace(/&(?!amp;|lt;|gt;|quot;|apos;|nbsp;)/g, '&amp;')
        .replace(/<(?!\/?math\b)(?!img\b)/g, '&lt;')
        .replace(/(?<!<\/math)(?<!<img[^>]*)>/g, '&gt;')
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

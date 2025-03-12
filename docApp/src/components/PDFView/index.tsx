import { useCallback,  useEffect, useState, useRef } from 'react';
import { useResizeObserver } from '@wojtekmaj/react-hooks';
import { pdfjs, Document, Page } from 'react-pdf';
import 'react-pdf/dist/esm/Page/AnnotationLayer.css';
import 'react-pdf/dist/esm/Page/TextLayer.css';

import './index.css';

import { DownOutlined, FilePdfOutlined, UpOutlined } from '@ant-design/icons';
import { FloatButton } from 'antd';

pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

const options = {
  standardFontDataUrl: 'https://unpkg.com/pdfjs-dist@${pdfjs.version}/standard_fonts',
};

const resizeObserverOptions = {};

const maxWidth = 600;

type PDFFile = string | File | null;

// @ts-ignore
function highlightPattern(text: string, pattern: string | null): string {
  if (!pattern) return text;
  
  // 创建text的副本，避免修改参数
  let result = text;
  
  //以空格分隔pattern
  const patternArr = pattern.split(' ').filter((p: string) => p.trim() !== '');
  
  //遍历替换
  for (let i = 0; i < patternArr.length; i++) {
    if (patternArr[i].trim() === '') continue;
    
    //让mark标签序号从2-6随机出现
    const randomNum = Math.floor(Math.random() * 5) + 2;
    
    // 创建不区分大小写的正则表达式
    const regex = new RegExp(patternArr[i], 'gi');
    result = result.replace(regex, (value: string) => `<mark${randomNum}>${value}</mark${randomNum}>`);
  }
  
  return result;
}

// 用于存储跨行关键词状态的接口
interface PartialMatch {
  keyword: string;
  firstPart: string;
  isSplitPoint: number;
}

const PDFView = (props: { file:string, searchText:string, pageNum:number } )=> {
  const [file, setFile] = useState<PDFFile>(null);
  const [pageNum, setPageNum] = useState<number>(1);
  const [numPages, setNumPages] = useState<number>(1);
  const [containerRef, setContainerRef] = useState<HTMLElement | null>(null);
  const [containerWidth, setContainerWidth] = useState<number>();
  const [searchText, setSearchText] = useState<string | null>(null);
  
  // 使用useRef替代state来存储不需要触发重新渲染的跨行匹配数据
  const lastLineTextRef = useRef<string>('');
  const partialMatchesRef = useRef<PartialMatch[]>([]);

  const onResize = useCallback<ResizeObserverCallback>((entries) => {
    const [entry] = entries;
    if (entry) {
      setContainerWidth(entry.contentRect.width);
    }
  }, []);
  useResizeObserver(containerRef, resizeObserverOptions, onResize);

  // 增强版textRenderer，支持跨行关键词匹配，但避免在回调中更新状态
  const textRenderer = useCallback(
    (textItem: { str: string }) => {
      if (!searchText) return textItem.str;
      
      const currentText = textItem.str;
      let highlightedText = currentText;
      
      // 1. 检查现有的部分匹配，看当前行是否包含关键词的后续部分
      const partialMatches = partialMatchesRef.current;
      if (partialMatches.length > 0) {
        const newPartialMatches = [...partialMatches];
        let matchFound = false;
        
        for (let i = partialMatches.length - 1; i >= 0; i--) {
          const match = partialMatches[i];
          const remainingPart = match.keyword.substring(match.isSplitPoint);
          
          // 如果当前行以关键词剩余部分开头
          if (currentText.startsWith(remainingPart)) {
            // 应用高亮
            const randomNum = Math.floor(Math.random() * 5) + 2;
            highlightedText = `<mark${randomNum}>${remainingPart}</mark${randomNum}>` + 
                              highlightedText.substring(remainingPart.length);
            
            // 移除这个匹配项，因为它已经完成了
            newPartialMatches.splice(i, 1);
            matchFound = true;
            break;
          }
          // 如果当前行的开头包含关键词的部分但不是全部
          else if (remainingPart.length > currentText.length) {
            // 检查当前行是否匹配关键词的一部分
            const currentPart = remainingPart.substring(0, currentText.length);
            if (currentText === currentPart) {
              // 更新部分匹配状态
              newPartialMatches[i] = {
                ...match,
                isSplitPoint: match.isSplitPoint + currentText.length
              };
              
              // 整行都应该高亮
              const randomNum = Math.floor(Math.random() * 5) + 2;
              highlightedText = `<mark${randomNum}>${currentText}</mark${randomNum}>`;
              matchFound = true;
              break;
            }
          }
        }
        
        // 更新部分匹配状态 - 但不使用setState，而是直接修改ref
        if (matchFound) {
          partialMatchesRef.current = newPartialMatches;
          lastLineTextRef.current = currentText;
          return highlightedText;
        }
      }
      
      // 2. 常规单行匹配
      highlightedText = highlightPattern(currentText, searchText);
      
      // 3. 检查是否有新的跨行匹配
      if (searchText) {
        const keywords = searchText.split(' ').filter((k: string) => k.trim() !== '' && k.length > 2);
        
        // 检查当前行末尾是否是某个关键词的开头
        for (const keyword of keywords) {
          // 只考虑长度足够的关键词
          if (keyword.length > 3) {
            // 从最长的可能前缀开始检查
            for (let prefixLength = Math.min(keyword.length - 2, currentText.length); prefixLength > 1; prefixLength--) {
              const prefix = currentText.substring(currentText.length - prefixLength);
              
              // 如果当前行末尾匹配关键词的前缀
              if (keyword.startsWith(prefix)) {
                // 添加到部分匹配列表 - 直接修改ref，不调用setState
                partialMatchesRef.current = [...partialMatchesRef.current, {
                  keyword,
                  firstPart: prefix,
                  isSplitPoint: prefix.length
                }];
                
                // 高亮当前行中匹配的部分
                const randomNum = Math.floor(Math.random() * 5) + 2;
                highlightedText = highlightedText.substring(0, highlightedText.length - prefixLength) + 
                                  `<mark${randomNum}>${prefix}</mark${randomNum}>`;
                break;
              }
            }
          }
        }
      }
      
      // 更新lastLineText - 直接修改ref，不调用setState
      lastLineTextRef.current = currentText;
      
      return highlightedText;
    },
    [searchText] // 只依赖searchText，不再依赖会导致循环的state
  );

  // @ts-ignore
  function onDocumentLoadSuccess({ numPages }: { numPages: number }): void {
    setNumPages(numPages);
    setPageNum(props.pageNum || 1);
    // 重置跨行匹配状态 - 使用ref直接修改
    lastLineTextRef.current = '';
    partialMatchesRef.current = [];
  }

  //监视file 和 searchText 参数变化时设置相关数据
  useEffect(() => {
    setFile(props.file);
    if (props.searchText !== null && props.searchText !== '') {
      setSearchText(props.searchText);
      // 搜索文本变化时重置跨行匹配状态 - 使用ref直接修改
      lastLineTextRef.current = '';
      partialMatchesRef.current = [];
    }
  }, [props.file, props.searchText]);

  // 单独监视pageNum的初始化
  useEffect(() => {
    setPageNum(props.pageNum);
    // 页面变化时重置跨行匹配状态 - 使用ref直接修改
    lastLineTextRef.current = '';
    partialMatchesRef.current = [];
  }, [props.pageNum]);

  // @ts-ignore
  return (
    <div className="Example__container__document" ref={setContainerRef}>
      <Document file={file} onLoadSuccess={onDocumentLoadSuccess} options={options}>
        <Page
          key={pageNum}
          pageNumber={pageNum}
          width={containerWidth ? Math.min(containerWidth, maxWidth) : maxWidth}
          customTextRenderer={textRenderer}
          className="pdf-page-center"
        >
        </Page>
      </Document>
      <FloatButton.Group shape="circle" style={{ insetInlineEnd: 24, position: 'fixed', right: '4%', transform: 'translateX(50%)', bottom: '45%' }}>
        <FloatButton 
          icon={<UpOutlined />} 
          onClick={() => {
            if (pageNum > 1) {
              setPageNum(pageNum - 1);
            }
          }}
        />
        <FloatButton icon={<FilePdfOutlined />} onClick={()=> {
          window.open(`${file}`, '_blank');
        }}/>
        <FloatButton 
          icon={<DownOutlined />} 
          onClick={() => {
            if (pageNum < numPages) {
              setPageNum(pageNum + 1);
            }
          }}
        />
      </FloatButton.Group>
    </div>
  );
}

export default PDFView;
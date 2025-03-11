declare module 'slash2';
declare module '*.css';
declare module '*.less' {
  const classes: {
    readonly [key: string]: string;
    readonly chatWindow: string;
    readonly messageList: string;
    readonly inputArea: string;
    readonly userMessage: string;
    readonly assistantMessage: string;
    readonly container: string;
    readonly chatArea: string;
    readonly promptArea: string;
    readonly promptPanel: string;
    readonly title: string;
  };
  export default classes;
}
declare module '*.scss' {
  const classes: { readonly [key: string]: string };
  export default classes;
}
declare module '*.sass' {
  const classes: { readonly [key: string]: string };
  export default classes;
}
declare module '*.svg' {
  export function ReactComponent(props: React.SVGProps<SVGSVGElement>): React.ReactElement;
  const url: string;
  export default url;
}
declare module '*.png';
declare module '*.jpg';
declare module '*.jpeg';
declare module '*.gif';
declare module '*.bmp';
declare module '*.tiff';
declare module 'omit.js';
declare module 'numeral';
declare module '@antv/data-set';
declare module 'mockjs';
declare module 'react-fittext';
declare module 'bizcharts-plugin-slider';

declare const REACT_APP_ENV: 'test' | 'dev' | 'pre' | false;

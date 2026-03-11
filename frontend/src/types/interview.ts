// 面试相关类型定义

export interface InterviewSession {
  jobId: number;
  sessionId: string;
  resumeText: string;
  totalQuestions: number;
  currentQuestionIndex: number;
  questions: InterviewQuestion[];
  status: 'CREATED' | 'IN_PROGRESS' | 'COMPLETED' | 'EVALUATED';
}

export interface InterviewQuestion {
  questionIndex: number;
  question: string;
  type: QuestionType;
  category: string;
  userAnswer: string | null;
  score: number | null;
  feedback: string | null;
  isFollowUp?: boolean;
  addQuestionIndex?: number;
  parentQuestionIndex?: number | null;
}

export type QuestionType = 
  | 'PROJECT' 
  | 'JAVA_BASIC' 
  | 'JAVA_COLLECTION' 
  | 'JAVA_CONCURRENT' 
  | 'MYSQL' 
  | 'REDIS' 
  | 'SPRING' 
  | 'SPRING_BOOT'
  | 'HTML_CSS'
  | 'JS_BASIC'
  | 'FRONT_FRAMEWORK'
  | 'BROWSER_NET'
  | 'FRONT_ENGINEERING'
  | 'TEST_CASE_DESIGN'
  | 'TEST_AUTOMATION'
  | 'TEST_PERFORMANCE'
  | 'TEST_DB_CHECK'
  | 'TEST_BUG_MANAGE';

export interface CreateInterviewRequest {
  resumeText: string;
  questionCount: number;
  resumeId?: number;
  jobId: number;
  forceCreate?: boolean;  // 是否强制创建新会话（忽略未完成的会话）
}

export interface SubmitAnswerRequest {
  sessionId: string;
  questionIndex: number;
  answer: string;
  addQuestionIndex: number;
}

export interface SubmitAnswerResponse {
  hasNextQuestion: boolean;
  nextQuestion: InterviewQuestion | null;
  currentIndex: number;
  // 后端字段名为 addQuestionIndex（拼写如此）
  addQuestionIndex?: number;
  totalQuestions: number;
}

export interface CurrentQuestionResponse {
  completed: boolean;
  question?: InterviewQuestion;
  message?: string;
}

export interface InterviewReport {
  sessionId: string;
  totalQuestions: number;
  overallScore: number;
  categoryScores: CategoryScore[];
  questionDetails: QuestionEvaluation[];
  overallFeedback: string;
  strengths: string[];
  improvements: string[];
  referenceAnswers: ReferenceAnswer[];
}

export interface CategoryScore {
  category: string;
  score: number;
  questionCount: number;
}

export interface QuestionEvaluation {
  questionIndex: number;
  question: string;
  category: string;
  userAnswer: string;
  score: number;
  feedback: string;
}

export interface ReferenceAnswer {
  questionIndex: number;
  question: string;
  referenceAnswer: string;
  keyPoints: string[];
}

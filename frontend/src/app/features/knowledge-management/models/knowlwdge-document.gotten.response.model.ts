import { KnowledgeDocumentGottenData } from './knowlwdge-document.gotten.model';

export interface KnowledgeDocumentGottenResource {
  code: string;
  message: string;
  data: KnowledgeDocumentGottenData[];
}

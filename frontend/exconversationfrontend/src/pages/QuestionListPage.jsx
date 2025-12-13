import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { uploadAPI, chapterAPI, questionAPI } from '../services/api';
import QuestionCard from '../components/Question/QuestionCard';
import './QuestionListPage.css';

function QuestionListPage() {
  const navigate = useNavigate();
  
  // Data state
  const [uploads, setUploads] = useState([]);
  const [uploadChapters, setUploadChapters] = useState({}); // { uploadId: [chapters] }
  const [chapterQuestions, setChapterQuestions] = useState({}); // { chapterId: [questions] }
  const [expandedChapters, setExpandedChapters] = useState(new Set()); // Set of expanded chapter IDs
  
  // Loading state
  const [loading, setLoading] = useState(true);
  const [loadingChapters, setLoadingChapters] = useState({}); // { uploadId: boolean }
  const [loadingQuestions, setLoadingQuestions] = useState({}); // { chapterId: boolean }
  
  // Lazy load state
  const [hasMore, setHasMore] = useState({}); // { chapterId: boolean }
  const [currentPage, setCurrentPage] = useState({}); // { chapterId: pageNumber }
  const pageSize = 20;
  
  useEffect(() => {
    loadUploads();
  }, []);

  const loadUploads = async () => {
    try {
      setLoading(true);
      const response = await uploadAPI.getAll();
      const parsedUploads = response.data.filter(u => u.status === 'parsed');
      setUploads(parsedUploads);
      
      // Load chapters for each upload
      for (const upload of parsedUploads) {
        loadChaptersForUpload(upload.id);
      }
    } catch (error) {
      console.error('Error loading uploads:', error);
      toast.error('Failed to load uploads: ' + error.message);
      setUploads([]);
    } finally {
      setLoading(false);
    }
  };

  const loadChaptersForUpload = async (uploadId) => {
    if (uploadChapters[uploadId]) return; // Already loaded
    
    try {
      setLoadingChapters(prev => ({ ...prev, [uploadId]: true }));
      const response = await chapterAPI.getByUploadId(uploadId);
      setUploadChapters(prev => ({ ...prev, [uploadId]: response.data }));
    } catch (error) {
      console.error('Error loading chapters:', error);
      toast.error('Failed to load chapters: ' + error.message);
    } finally {
      setLoadingChapters(prev => ({ ...prev, [uploadId]: false }));
    }
  };

  const loadQuestionsForChapter = async (chapterId, page = 0, append = false) => {
    try {
      setLoadingQuestions(prev => ({ ...prev, [chapterId]: true }));
      const response = await questionAPI.getAll(chapterId, page, pageSize);
      
      console.log('QuestionListPage: API response for chapter', chapterId, ':', response.data);
      
      let questions = [];
      let hasMoreData = false;
      
      if (response.data.content) {
        questions = response.data.content;
        hasMoreData = page < response.data.totalPages - 1;
      } else if (Array.isArray(response.data)) {
        questions = response.data;
        hasMoreData = false;
      }
      
      console.log('QuestionListPage: Parsed questions:', questions);
      if (questions.length > 0) {
        console.log('QuestionListPage: First question structure:', questions[0]);
        console.log('QuestionListPage: First question contentLatex:', questions[0].contentLatex);
        console.log('QuestionListPage: First question answers:', questions[0].answers);
      }
      
      if (append) {
        setChapterQuestions(prev => ({
          ...prev,
          [chapterId]: [...(prev[chapterId] || []), ...questions]
        }));
      } else {
        setChapterQuestions(prev => ({
          ...prev,
          [chapterId]: questions
        }));
      }
      
      setHasMore(prev => ({ ...prev, [chapterId]: hasMoreData }));
      setCurrentPage(prev => ({ ...prev, [chapterId]: page }));
    } catch (error) {
      console.error('Error loading questions:', error);
      toast.error('Failed to load questions: ' + error.message);
    } finally {
      setLoadingQuestions(prev => ({ ...prev, [chapterId]: false }));
    }
  };

  const handleChapterToggle = (chapterId) => {
    const isExpanded = expandedChapters.has(chapterId);
    
    if (isExpanded) {
      // Collapse
      setExpandedChapters(prev => {
        const newSet = new Set(prev);
        newSet.delete(chapterId);
        return newSet;
      });
    } else {
      // Expand - load questions if not loaded
      setExpandedChapters(prev => new Set(prev).add(chapterId));
      
      if (!chapterQuestions[chapterId]) {
        loadQuestionsForChapter(chapterId, 0, false);
      }
    }
  };

  // Infinite scroll handler
  const loadMoreQuestions = useCallback((chapterId) => {
    if (loadingQuestions[chapterId] || !hasMore[chapterId]) return;
    
    const nextPage = (currentPage[chapterId] || 0) + 1;
    loadQuestionsForChapter(chapterId, nextPage, true);
  }, [loadingQuestions, hasMore, currentPage]);

  // Component for load more trigger with observer
  const LoadMoreTrigger = ({ chapterId }) => {
    const triggerRef = useRef(null);

    useEffect(() => {
      const observer = new IntersectionObserver(
        (entries) => {
          if (entries[0].isIntersecting) {
            loadMoreQuestions(chapterId);
          }
        },
        { threshold: 0.1 }
      );

      if (triggerRef.current) {
        observer.observe(triggerRef.current);
      }

      return () => {
        if (triggerRef.current) {
          observer.unobserve(triggerRef.current);
        }
      };
    }, [chapterId, loadMoreQuestions]);

    return (
      <div
        ref={triggerRef}
        className="load-more-trigger"
      >
        {loadingQuestions[chapterId] ? 'Loading more...' : ''}
      </div>
    );
  };

  const handleEdit = (id) => {
    navigate(`/questions/${id}/edit`);
  };

  const handleDelete = async (id, chapterId) => {
    if (!window.confirm('Are you sure you want to delete this question?')) {
      return;
    }
    try {
      await questionAPI.delete(id);
      toast.success('Question deleted successfully');
      // Remove from local state
      setChapterQuestions(prev => ({
        ...prev,
        [chapterId]: (prev[chapterId] || []).filter(q => q.id !== id)
      }));
    } catch (error) {
      toast.error('Failed to delete question: ' + error.message);
    }
  };

  if (loading) {
    return <div className="loading">Loading uploads...</div>;
  }

  return (
    <div className="question-list-page">
      <div className="page-header">
        <h1>Đề đã tải lên</h1>
      </div>

      {uploads.length === 0 ? (
        <div className="empty-state">
          <p>Chưa có đề nào được tải lên. Vui lòng upload file DOCX trước.</p>
        </div>
      ) : (
        <div className="uploads-list-paper">
          {uploads.map((upload) => (
            <div key={upload.id} className="upload-paper-card">
              {/* Paper Header */}
              <div className="paper-header">
                <div className="paper-title">
                  <h2>{upload.fileName}</h2>
                  <span className={`status-badge status-${upload.status}`}>
                    {upload.status}
                  </span>
                </div>
                <div className="paper-meta">
                  <span>Uploaded by: {upload.uploadedByName || 'System'}</span>
                  <span>Date: {new Date(upload.uploadDate).toLocaleString()}</span>
                </div>
              </div>

              {/* Chapters List */}
              <div className="paper-chapters">
                {loadingChapters[upload.id] ? (
                  <div className="loading-chapters">Loading chapters...</div>
                ) : uploadChapters[upload.id]?.length > 0 ? (
                  uploadChapters[upload.id].map((chapter) => {
                    const isExpanded = expandedChapters.has(chapter.id);
                    const questions = chapterQuestions[chapter.id] || [];
                    const isLoading = loadingQuestions[chapter.id];
                    const hasMoreData = hasMore[chapter.id];

                    return (
                      <div key={chapter.id} className="chapter-row">
                        {/* Chapter Header - Clickable */}
                        <div
                          className="chapter-row-header"
                          onClick={() => handleChapterToggle(chapter.id)}
                        >
                          <div className="chapter-row-title">
                            <span className="expand-arrow" data-expanded={isExpanded}>
                              ▶
                            </span>
                            <span className="chapter-name">{chapter.chapterName}</span>
                            {chapter.chapterIndex && (
                              <span className="chapter-index">#{chapter.chapterIndex}</span>
                            )}
                          </div>
                          <span className="chapter-question-count">
                            {questions.length > 0 && `(${questions.length} câu hỏi)`}
                          </span>
                        </div>

                        {/* Expanded Questions */}
                        {isExpanded && (
                          <div className="chapter-questions">
                            {isLoading && questions.length === 0 ? (
                              <div className="loading-questions">Loading questions...</div>
                            ) : questions.length > 0 ? (
                              <>
                                {questions.map((question) => (
                                  <QuestionCard
                                    key={question.id}
                                    question={question}
                                    onUpdate={() => loadQuestionsForChapter(chapter.id, currentPage[chapter.id] || 0, false)}
                                    onDelete={() => handleDelete(question.id, chapter.id)}
                                  />
                                ))}
                                
                                {/* Infinite Scroll Trigger */}
                                {hasMoreData && <LoadMoreTrigger chapterId={chapter.id} />}
                              </>
                            ) : (
                              <div className="empty-questions">Chưa có câu hỏi nào trong chương này.</div>
                            )}
                          </div>
                        )}
                      </div>
                    );
                  })
                ) : (
                  <div className="no-chapters">Chưa có chương nào trong đề này.</div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default QuestionListPage;

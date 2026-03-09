import { useState, useEffect } from 'react';
import { questionAPI } from '../../services/api';
import { toast } from 'react-toastify';
import MathContentRenderer from './MathContentRenderer';
import './QuestionCard.css';

function QuestionCard({ question, onUpdate, onDelete }) {
  const [isEditing, setIsEditing] = useState(false);
  const [editedData, setEditedData] = useState({
    title: question?.title || '',
    contentMathml: question?.contentMathml || '',
    contentOmml: question?.contentOmml || '',
    chapterName: question?.chapterName || '',
    answers: question?.answers || [],
    images: question?.images || [],
  });
  const [saving, setSaving] = useState(false);

  // Sync editedData when question prop changes
  useEffect(() => {
    if (question && !isEditing) {
      console.log('QuestionCard: Updating data from question prop:', question);
      console.log('QuestionCard: ContentMathml:', question.contentMathml ? `Length: ${question.contentMathml.length}` : 'null/undefined');
      console.log('QuestionCard: Answers:', question.answers ? `Count: ${question.answers.length}` : 'null/undefined');
      setEditedData({
    title: question.title || '',
        contentMathml: question.contentMathml || '',
        contentOmml: question.contentOmml || '',
    chapterName: question.chapterName || '',
    answers: question.answers || [],
    images: question.images || [],
  });
    }
  }, [question, isEditing]);

  // Sort answers by order label (A, B, C, D)
  const sortedAnswers = [...(editedData.answers || [])].sort((a, b) => {
    return (a.orderLabel || '').localeCompare(b.orderLabel || '');
  });

  const handleEdit = () => {
    setIsEditing(true);
  };

  const handleCancel = () => {
    setEditedData({
      title: question.title || '',
      contentMathml: question.contentMathml || '',
      contentOmml: question.contentOmml || '',
      chapterName: question.chapterName || '',
      answers: question.answers || [],
      images: question.images || [],
    });
    setIsEditing(false);
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      
      // Prepare update data
      const updateData = {
        title: editedData.title,
        contentMathml: editedData.contentMathml,
        contentOmml: editedData.contentOmml,
        chapterName: editedData.chapterName,
        answers: editedData.answers.map((answer, index) => ({
          id: answer.id,
          orderLabel: answer.orderLabel || ['A', 'B', 'C', 'D'][index] || 'A',
          contentMathml: answer.contentMathml || '',
          contentOmml: answer.contentOmml || '',
          isCorrect: answer.isCorrect || false,
        })),
      };

      await questionAPI.update(question.id, updateData);
      toast.success('Question updated successfully');
      setIsEditing(false);
      if (onUpdate) {
        onUpdate();
      }
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || error.message || 'Unknown error';
      toast.error('Failed to update question: ' + errorMessage);
    } finally {
      setSaving(false);
    }
  };

  const handleAnswerChange = (index, field, value) => {
    const newAnswers = [...editedData.answers];
    if (!newAnswers[index]) {
      newAnswers[index] = {
        id: null,
        orderLabel: ['A', 'B', 'C', 'D'][index] || 'A',
        contentMathml: '',
        contentOmml: '',
        isCorrect: false,
      };
    }
    newAnswers[index][field] = value;
    setEditedData({ ...editedData, answers: newAnswers });
  };

  const handleContentChange = (field, value) => {
    setEditedData({ ...editedData, [field]: value });
  };

  const handleDelete = async () => {
    if (!window.confirm('Are you sure you want to delete this question?')) {
      return;
    }
    try {
      await questionAPI.delete(question.id);
      toast.success('Question deleted successfully');
      if (onDelete) {
        onDelete();
      }
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || error.message || 'Unknown error';
      toast.error('Failed to delete question: ' + errorMessage);
    }
  };

  // Extract section and chapter from chapterName if format is "Phần X - Chương Y" or similar
  const extractSectionAndChapter = (chapterName) => {
    if (!chapterName) return { section: null, chapter: null };
    
    // Try to match patterns like "Phần 1 - Chương 1" or "Phần I - Chương I"
    const sectionMatch = chapterName.match(/(?:Phần|PHẦN|Part)\s*([\dIVX]+)/i);
    const chapterMatch = chapterName.match(/(?:Chương|CHƯƠNG|Chapter)\s*([\dIVX]+)/i);
    
    return {
      section: sectionMatch ? sectionMatch[0] : null,
      chapter: chapterMatch ? chapterMatch[0] : (chapterName || null),
    };
  };

  const { section, chapter } = extractSectionAndChapter(editedData.chapterName);
  
  // Show section/chapter in card only if not already shown in group header
  const showSectionInCard = false; // Will be controlled by parent if needed

  return (
    <div className="question-card-editable">
      <div className="question-header-editable">
        <div className="question-meta">
          {(section || chapter) && (
            <div className="section-chapter-info">
              {section && (
                <div className="section-info">
                  <label>Phần:</label>
                  {isEditing ? (
                    <input
                      type="text"
                      value={section}
                      onChange={(e) => {
                        const newChapterName = chapter 
                          ? `${e.target.value} - ${chapter}`
                          : e.target.value;
                        handleContentChange('chapterName', newChapterName);
                      }}
                      className="section-input"
                      placeholder="Phần 1"
                    />
                  ) : (
                    <span className="section-label">{section}</span>
                  )}
                </div>
              )}
              {chapter && (
                <div className="chapter-info">
                  <label>Chương:</label>
                  {isEditing ? (
                    <input
                      type="text"
                      value={chapter}
                      onChange={(e) => {
                        const newChapterName = section
                          ? `${section} - ${e.target.value}`
                          : e.target.value;
                        handleContentChange('chapterName', newChapterName);
                      }}
                      className="chapter-input"
                      placeholder="Chương 1"
                    />
                  ) : (
                    <span className="chapter-label">{chapter}</span>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
        <div className="question-actions-editable">
          {isEditing ? (
            <>
              <button onClick={handleSave} className="btn-save" disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
              </button>
              <button onClick={handleCancel} className="btn-cancel" disabled={saving}>
                Cancel
              </button>
            </>
          ) : (
            <>
              <button onClick={handleEdit} className="btn-edit">
                Edit
              </button>
              <button onClick={handleDelete} className="btn-delete">
                Delete
              </button>
            </>
          )}
        </div>
      </div>

      <div className="question-content-editable">
        <div className="question-title-section">
          <label>Câu hỏi:</label>
          {isEditing ? (
            <input
              type="text"
              value={editedData.title}
              onChange={(e) => handleContentChange('title', e.target.value)}
              className="question-title-input"
              placeholder="Câu 1"
            />
          ) : (
            <span className="question-title">{editedData.title || `Câu ${question.id}`}</span>
          )}
        </div>

        <div className="question-content-section">
          <label>Nội dung (MathML):</label>
          {isEditing ? (
            <textarea
              value={editedData.contentMathml || ''}
              onChange={(e) => handleContentChange('contentMathml', e.target.value)}
              className="question-content-textarea"
              placeholder="Nhập nội dung câu hỏi..."
              rows={4}
            />
          ) : (
            <MathContentRenderer 
              content={editedData.contentMathml || ''}
              images={editedData.images || []}
              className="question-content-display"
            />
          )}
          {isEditing && (
            <>
              <label>Nội dung (OMML):</label>
              <textarea
                value={editedData.contentOmml || ''}
                onChange={(e) => handleContentChange('contentOmml', e.target.value)}
                className="question-content-textarea"
                placeholder="OMML (nếu cần giữ cho export DOCX)"
                rows={4}
              />
            </>
          )}
        </div>

        {/* Images fallback cho câu hỏi cũ chưa có placeholder [IMAGE:] trong content */}
        {editedData.images && editedData.images.length > 0 &&
          !(editedData.contentMathml || '').includes('[IMAGE:') && (
          <div className="question-images-section">
            <label>Hình ảnh:</label>
            <div className="images-grid">
              {editedData.images.map((image, idx) => {
                const getImageUrl = (imagePath) => {
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
                };
                return (
                  <div key={image.id || idx} className="image-item">
                    <img
                      src={getImageUrl(image.imagePath)}
                      alt={image.description || 'Question image'}
                      className="question-image"
                      onError={(e) => { e.target.style.display = 'none'; }}
                    />
                    {image.description && (
                      <div className="image-description">{image.description}</div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Answers */}
        <div className="question-answers-section">
          <label>Đáp án:</label>
          <div className="answers-list">
            {['A', 'B', 'C', 'D'].map((label, index) => {
              const answer = sortedAnswers.find(a => a.orderLabel === label) || {
                id: null,
                orderLabel: label,
                contentMathml: '',
                contentOmml: '',
                isCorrect: false,
              };

              return (
                <div key={label} className="answer-item">
                  <div className="answer-label-section">
                    <span className="answer-label">{label}.</span>
                    {isEditing ? (
                      <>
                        <label>Đáp án {label} (MathML)</label>
                        <textarea
                          value={answer.contentMathml || ''}
                          onChange={(e) => handleAnswerChange(index, 'contentMathml', e.target.value)}
                          className="answer-content-textarea"
                          placeholder={`Nhập MathML đáp án ${label}...`}
                          rows={2}
                        />
                        <label>Đáp án {label} (OMML)</label>
                        <textarea
                          value={answer.contentOmml || ''}
                          onChange={(e) => handleAnswerChange(index, 'contentOmml', e.target.value)}
                          className="answer-content-textarea"
                          placeholder={`Nhập OMML đáp án ${label}...`}
                          rows={2}
                        />
                        <label className="correct-answer-checkbox">
                          <input
                            type="checkbox"
                            checked={answer.isCorrect || false}
                            onChange={(e) => handleAnswerChange(index, 'isCorrect', e.target.checked)}
                          />
                          Đáp án đúng
                        </label>
                      </>
                    ) : (
                      <>
                        <div className={`answer-content ${answer.isCorrect ? 'correct-answer' : ''}`}>
                          <MathContentRenderer 
                            content={answer.contentMathml || `(Chưa có nội dung đáp án ${label})`}
                          />
                        </div>
                        {answer.isCorrect && (
                          <span className="correct-badge">✓ Đúng</span>
                        )}
                      </>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}

export default QuestionCard;


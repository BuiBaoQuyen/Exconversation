import { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { examAPI } from '../services/api';
import ExamList from '../components/Exam/ExamList';
import ExamView from '../components/Exam/ExamView';
import './ExamPage.css';

function ExamPage() {
  const [exams, setExams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showExamView, setShowExamView] = useState(false);
  const [selectedExam, setSelectedExam] = useState(null);
  const [showPrintDialog, setShowPrintDialog] = useState(false);
  const [numberOfQuestions, setNumberOfQuestions] = useState(40);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const examsRes = await examAPI.getAll();
      setExams(examsRes.data);
    } catch (error) {
      toast.error('Failed to load data: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleViewExam = (exam) => {
    setSelectedExam(exam);
    setShowExamView(true);
  };

  const handleExportExam = async (examId, includeAnswers) => {
    try {
      const response = await examAPI.export(examId, includeAnswers);
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `exam_${examId}.docx`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      toast.success('Exam exported successfully');
    } catch (error) {
      toast.error('Failed to export exam: ' + error.message);
    }
  };

  const handlePrintRandomExam = async (numQuestions, includeAnswers = false) => {
    console.log('[ExamPage] handlePrintRandomExam called', { numQuestions, includeAnswers });
    try {
      console.log('[ExamPage] Calling examAPI.printRandom...');
      const response = await examAPI.printRandom(numQuestions, includeAnswers);
      console.log('[ExamPage] API response received', {
        status: response.status,
        headers: response.headers,
        dataType: typeof response.data,
        dataSize: response.data?.size || response.data?.length || 'unknown'
      });
      
      if (!response.data) {
        console.error('[ExamPage] Response data is empty!');
        toast.error('Không nhận được dữ liệu từ server');
        return;
      }
      
      console.log('[ExamPage] Creating blob URL...');
      const url = window.URL.createObjectURL(new Blob([response.data]));
      console.log('[ExamPage] Blob URL created:', url);
      
      const link = document.createElement('a');
      link.href = url;
      const timestamp = new Date().getTime();
      const filename = `exam_random_${timestamp}.docx`;
      link.setAttribute('download', filename);
      console.log('[ExamPage] Download link created with filename:', filename);
      
      document.body.appendChild(link);
      console.log('[ExamPage] Triggering download...');
      link.click();
      link.remove();
      console.log('[ExamPage] Download completed successfully');
      toast.success(`Đề thi mới với ${numQuestions} câu hỏi đã được tạo và xuất thành công!`);
      setShowPrintDialog(false);
    } catch (error) {
      console.error('[ExamPage] Error in handlePrintRandomExam:', error);
      console.error('[ExamPage] Error details:', {
        message: error.message,
        response: error.response,
        status: error.response?.status,
        data: error.response?.data,
        stack: error.stack
      });
      
      let errorMessage = 'Không thể tạo đề thi';
      if (error.response?.status === 400) {
        errorMessage = 'Số câu hỏi không hợp lệ hoặc không đủ câu hỏi trong database.';
      } else if (error.message) {
        errorMessage = 'Không thể tạo đề thi: ' + error.message;
      }
      
      toast.error(errorMessage);
    }
  };

  const handlePrintDialogSubmit = (e) => {
    e.preventDefault();
    if (numberOfQuestions <= 0) {
      toast.error('Số câu hỏi phải lớn hơn 0');
      return;
    }
    handlePrintRandomExam(numberOfQuestions, false);
  };

  const handleExamDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this exam?')) {
      return;
    }
    try {
      await examAPI.delete(id);
      toast.success('Exam deleted successfully');
      loadData();
    } catch (error) {
      toast.error('Failed to delete exam: ' + error.message);
    }
  };

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  return (
    <div className="exam-page">
      <div className="page-header">
        <h1>Exam Management</h1>
        <div className="header-actions">
          <button
            onClick={(e) => {
              console.log('[ExamPage] Print button clicked', e);
              setShowPrintDialog(true);
            }}
            className="btn-primary"
            title="Tạo đề thi mới với số câu hỏi tùy chọn"
          >
            📄 Print Đề Thi Mới
          </button>
        </div>
      </div>

        <div className="tab-content">
          <div className="section-header">
            <h2>Generated Exams</h2>
          </div>
          <ExamList
            exams={exams}
            onView={handleViewExam}
            onExport={handleExportExam}
            onDelete={handleExamDelete}
          />
        </div>

      {showExamView && selectedExam && (
        <ExamView
          exam={selectedExam}
          onExport={handleExportExam}
          onClose={() => {
            setShowExamView(false);
            setSelectedExam(null);
          }}
        />
      )}

      {showPrintDialog && (
        <div className="modal-overlay" onClick={() => setShowPrintDialog(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Tạo Đề Thi Mới</h2>
              <button
                onClick={() => setShowPrintDialog(false)}
                className="btn-close"
              >
                ×
              </button>
            </div>
            <form onSubmit={handlePrintDialogSubmit} className="modal-body">
              <div className="form-group">
                <label htmlFor="numberOfQuestions">
                  Số câu hỏi muốn tạo:
                </label>
                <input
                  type="number"
                  id="numberOfQuestions"
                  min="1"
                  max="1000"
                  value={numberOfQuestions}
                  onChange={(e) => setNumberOfQuestions(parseInt(e.target.value) || 40)}
                  required
                  className="form-input"
                  autoFocus
                />
                <small className="form-hint">
                  Hệ thống sẽ lấy ngẫu nhiên số câu hỏi này từ database. 
                  Nếu không đủ, sẽ lấy tất cả câu hỏi có sẵn.
                </small>
              </div>
              <div className="modal-actions">
                <button
                  type="button"
                  onClick={() => setShowPrintDialog(false)}
                  className="btn-secondary"
                >
                  Hủy
                </button>
                <button type="submit" className="btn-primary">
                  Tạo Đề Thi
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default ExamPage;

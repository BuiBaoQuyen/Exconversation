import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { questionAPI } from '../services/api';
import QuestionList from '../components/Question/QuestionList';
import './QuestionListPage.css';

function QuestionListPage() {
  const navigate = useNavigate();
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterChapter, setFilterChapter] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    loadQuestions();
  }, [filterChapter]);

  const loadQuestions = async () => {
    try {
      setLoading(true);
      const response = await questionAPI.getAll(filterChapter);
      setQuestions(response.data);
    } catch (error) {
      toast.error('Failed to load questions: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (id) => {
    navigate(`/questions/${id}/edit`);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this question?')) {
      return;
    }
    try {
      await questionAPI.delete(id);
      toast.success('Question deleted successfully');
      loadQuestions();
    } catch (error) {
      toast.error('Failed to delete question: ' + error.message);
    }
  };

  const filteredQuestions = questions.filter((q) => {
    if (searchTerm) {
      const searchLower = searchTerm.toLowerCase();
      return (
        q.title?.toLowerCase().includes(searchLower) ||
        q.content?.toLowerCase().includes(searchLower) ||
        q.chapterName?.toLowerCase().includes(searchLower)
      );
    }
    return true;
  });

  if (loading) {
    return <div className="loading">Loading questions...</div>;
  }

  return (
    <div className="question-list-page">
      <div className="page-header">
        <h1>Question Management</h1>
      </div>

      <div className="filters">
        <input
          type="text"
          placeholder="Search questions..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="search-input"
        />
        <button
          onClick={() => setFilterChapter(null)}
          className={filterChapter === null ? 'btn-active' : 'btn-secondary'}
        >
          All Chapters
        </button>
      </div>

      <QuestionList
        questions={filteredQuestions}
        onEdit={handleEdit}
        onDelete={handleDelete}
      />
    </div>
  );
}

export default QuestionListPage;


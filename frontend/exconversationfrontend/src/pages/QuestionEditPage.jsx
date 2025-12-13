import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { questionAPI } from '../services/api';
import QuestionForm from '../components/Question/QuestionForm';
import './QuestionEditPage.css';

function QuestionEditPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [question, setQuestion] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadQuestion();
  }, [id]);

  const loadQuestion = async () => {
    try {
      setLoading(true);
      console.log('Loading question with ID:', id);
      const response = await questionAPI.getById(id);
      console.log('Question loaded:', response.data);
      console.log('ContentMathml length:', response.data?.contentMathml?.length || 0);
      console.log('Answers count:', response.data?.answers?.length || 0);
      setQuestion(response.data);
    } catch (error) {
      console.error('Error loading question:', error);
      console.error('Error response:', error.response?.data);
      toast.error('Failed to load question: ' + (error.response?.data?.message || error.message));
      navigate('/questions');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (formData) => {
    try {
      setSaving(true);
      await questionAPI.update(id, formData);
      toast.success('Question updated successfully');
      navigate('/questions');
    } catch (error) {
      toast.error('Failed to save question: ' + error.message);
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    navigate('/questions');
  };

  if (loading) {
    return <div className="loading">Loading question...</div>;
  }

  if (!question) {
    return <div className="error">Question not found</div>;
  }

  return (
    <div className="question-edit-page">
      <div className="page-header">
        <h1>Edit Question</h1>
        <button onClick={handleCancel} className="btn-secondary">
          Cancel
        </button>
      </div>
      <QuestionForm
        question={question}
        onSave={handleSave}
        onCancel={handleCancel}
        saving={saving}
      />
    </div>
  );
}

export default QuestionEditPage;


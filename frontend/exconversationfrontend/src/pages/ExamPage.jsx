import { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { blueprintAPI, examAPI } from '../services/api';
import BlueprintList from '../components/Exam/BlueprintList';
import ExamList from '../components/Exam/ExamList';
import BlueprintForm from '../components/Exam/BlueprintForm';
import ExamGenerator from '../components/Exam/ExamGenerator';
import ExamView from '../components/Exam/ExamView';
import './ExamPage.css';

function ExamPage() {
  const [blueprints, setBlueprints] = useState([]);
  const [exams, setExams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('blueprints'); // 'blueprints', 'exams'
  const [showBlueprintForm, setShowBlueprintForm] = useState(false);
  const [showExamGenerator, setShowExamGenerator] = useState(false);
  const [showExamView, setShowExamView] = useState(false);
  const [selectedBlueprint, setSelectedBlueprint] = useState(null);
  const [selectedExam, setSelectedExam] = useState(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [blueprintsRes, examsRes] = await Promise.all([
        blueprintAPI.getAll(),
        examAPI.getAll(),
      ]);
      setBlueprints(blueprintsRes.data);
      setExams(examsRes.data);
    } catch (error) {
      toast.error('Failed to load data: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateBlueprint = () => {
    setSelectedBlueprint(null);
    setShowBlueprintForm(true);
  };

  const handleEditBlueprint = (blueprint) => {
    setSelectedBlueprint(blueprint);
    setShowBlueprintForm(true);
  };

  const handleBlueprintSave = async (data) => {
    try {
      if (selectedBlueprint) {
        await blueprintAPI.update(selectedBlueprint.id, data);
        toast.success('Blueprint updated successfully');
      } else {
        await blueprintAPI.create(data);
        toast.success('Blueprint created successfully');
      }
      setShowBlueprintForm(false);
      setSelectedBlueprint(null);
      loadData();
    } catch (error) {
      toast.error('Failed to save blueprint: ' + error.message);
    }
  };

  const handleBlueprintDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this blueprint?')) {
      return;
    }
    try {
      await blueprintAPI.delete(id);
      toast.success('Blueprint deleted successfully');
      loadData();
    } catch (error) {
      toast.error('Failed to delete blueprint: ' + error.message);
    }
  };

  const handleGenerateExam = (blueprint) => {
    setSelectedBlueprint(blueprint);
    setShowExamGenerator(true);
  };

  const handleExamGenerate = async (blueprintId, examName) => {
    try {
      await examAPI.generate(blueprintId, examName, 'System');
      toast.success('Exam generated successfully');
      setShowExamGenerator(false);
      setSelectedBlueprint(null);
      setActiveTab('exams');
      loadData();
    } catch (error) {
      toast.error('Failed to generate exam: ' + error.message);
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
            onClick={() => setActiveTab('blueprints')}
            className={activeTab === 'blueprints' ? 'btn-active' : 'btn-secondary'}
          >
            Blueprints
          </button>
          <button
            onClick={() => setActiveTab('exams')}
            className={activeTab === 'exams' ? 'btn-active' : 'btn-secondary'}
          >
            Exams
          </button>
        </div>
      </div>

      {activeTab === 'blueprints' && (
        <div className="tab-content">
          <div className="section-header">
            <h2>Blueprints</h2>
            <button onClick={handleCreateBlueprint} className="btn-primary">
              Create Blueprint
            </button>
          </div>
          {showBlueprintForm && (
            <BlueprintForm
              blueprint={selectedBlueprint}
              onSubmit={handleBlueprintSave}
              onCancel={() => {
                setShowBlueprintForm(false);
                setSelectedBlueprint(null);
              }}
            />
          )}
          <BlueprintList
            blueprints={blueprints}
            onEdit={handleEditBlueprint}
            onDelete={handleBlueprintDelete}
            onGenerate={handleGenerateExam}
          />
        </div>
      )}

      {activeTab === 'exams' && (
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
      )}

      {showExamGenerator && selectedBlueprint && (
        <ExamGenerator
          blueprint={selectedBlueprint}
          onGenerate={handleExamGenerate}
          onCancel={() => {
            setShowExamGenerator(false);
            setSelectedBlueprint(null);
          }}
        />
      )}

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
    </div>
  );
}

export default ExamPage;


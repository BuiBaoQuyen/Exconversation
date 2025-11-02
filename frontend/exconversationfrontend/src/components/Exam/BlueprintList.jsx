import './BlueprintList.css';

function BlueprintList({ blueprints, onEdit, onDelete, onGenerate }) {
  if (blueprints.length === 0) {
    return (
      <div className="empty-state">
        <p>No blueprints found. Create your first blueprint to get started.</p>
      </div>
    );
  }

  return (
    <div className="blueprint-list">
      {blueprints.map((blueprint) => (
        <div key={blueprint.id} className="blueprint-card">
          <div className="blueprint-header">
            <h3>{blueprint.name}</h3>
          </div>
          <div className="blueprint-info">
            <p>
              <strong>Total Questions:</strong> {blueprint.totalQuestions}
            </p>
            {blueprint.description && (
              <p>
                <strong>Description:</strong> {blueprint.description}
              </p>
            )}
            {blueprint.details && blueprint.details.length > 0 && (
              <div className="blueprint-details">
                <strong>Chapter Distribution:</strong>
                <ul>
                  {blueprint.details.map((detail) => (
                    <li key={detail.id}>
                      {detail.chapterName}: {detail.percentage}% (
                      {Math.round((blueprint.totalQuestions * detail.percentage) / 100)} questions)
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
          <div className="blueprint-actions">
            <button onClick={() => onGenerate(blueprint)} className="btn-primary">
              Generate Exam
            </button>
            <button onClick={() => onEdit(blueprint)} className="btn-secondary">
              Edit
            </button>
            <button onClick={() => onDelete(blueprint.id)} className="btn-danger">
              Delete
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}

export default BlueprintList;

